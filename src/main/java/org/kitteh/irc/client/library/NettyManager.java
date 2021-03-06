/*
 * * Copyright (C) 2013-2015 Matt Baxter http://kitteh.org
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies
 * of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS
 * BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN
 * ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.kitteh.irc.client.library;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.codec.MessageToMessageEncoder;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.handler.ssl.IdentityCipherSuiteFilter;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.CharsetUtil;
import io.netty.util.concurrent.ScheduledFuture;
import org.kitteh.irc.client.library.event.client.ClientConnectionClosedEvent;
import org.kitteh.irc.client.library.exception.KittehConnectionException;

import javax.net.ssl.SSLException;
import java.io.File;
import java.net.SocketAddress;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

final class NettyManager {
    static class ClientConnection {
        private final IRCClient client;
        private final Channel channel;
        private final Queue<String> queue = new ConcurrentLinkedQueue<>();
        private boolean reconnect = true;
        private ScheduledFuture<?> scheduledSending;
        private final Object scheduledSendingLock = new Object();

        private ClientConnection(final IRCClient client, ChannelFuture future) {
            this.client = client;
            this.channel = future.channel();

            try {
                future.sync();
            } catch (InterruptedException e) {
                this.client.getExceptionListener().queue(e);
                return;
            }

            // Outbound - Processed in pipeline back to front.
            this.channel.pipeline().addFirst("[OUTPUT] Output listener", new MessageToMessageEncoder<String>() {
                @Override
                protected void encode(ChannelHandlerContext ctx, String msg, List<Object> out) throws Exception {
                    ClientConnection.this.client.getOutputListener().queue(msg);
                    out.add(msg);
                }
            });
            this.channel.pipeline().addFirst("[OUTPUT] Add line breaks", new MessageToMessageEncoder<String>() {
                @Override
                protected void encode(ChannelHandlerContext ctx, String msg, List<Object> out) throws Exception {
                    out.add(msg + "\r\n");
                }
            });
            this.channel.pipeline().addFirst("[OUTPUT] String encoder", new StringEncoder(CharsetUtil.UTF_8));

            // Handle timeout
            this.channel.pipeline().addLast("[INPUT] Idle state handler", new IdleStateHandler(250, 0, 60));
            this.channel.pipeline().addLast("[INPUT] Catch idle", new ChannelDuplexHandler() {
                @Override
                public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
                    if (evt instanceof IdleStateEvent) {
                        IdleStateEvent e = (IdleStateEvent) evt;
                        if (e.state() == IdleState.READER_IDLE && e.isFirst()) {
                            ClientConnection.this.shutdown("Reconnecting...", true);
                        } else if (e.state() == IdleState.ALL_IDLE && e.isFirst()) {
                            ClientConnection.this.client.ping();
                        }
                    }
                }
            });

            // Inbound
            this.channel.pipeline().addLast("[INPUT] Line splitter", new DelimiterBasedFrameDecoder(512, Unpooled.wrappedBuffer(new byte[]{'\r', '\n'})));
            this.channel.pipeline().addLast("[INPUT] String decoder", new StringDecoder(CharsetUtil.UTF_8));
            this.channel.pipeline().addLast("[INPUT] Send to client", new SimpleChannelInboundHandler<String>() {
                @Override
                protected void channelRead0(ChannelHandlerContext ctx, String msg) throws Exception {
                    ClientConnection.this.client.getInputListener().queue(msg);
                    ClientConnection.this.client.processLine(msg);
                }
            });

            // SSL
            if (this.client.getConfig().get(Config.SSL)) {
                try {
                    File keyCertChainFile = this.client.getConfig().get(Config.SSL_KEY_CERT_CHAIN);
                    File keyFile = this.client.getConfig().get(Config.SSL_KEY);
                    String keyPassword = this.client.getConfig().get(Config.SSL_KEY_PASSWORD);
                    SslContext sslContext = SslContext.newClientContext(null, null, new NettyTrustManagerFactory(this.client), keyCertChainFile, keyFile, keyPassword, null, null, IdentityCipherSuiteFilter.INSTANCE, null, 0, 0);
                    this.channel.pipeline().addFirst(sslContext.newHandler(this.channel.alloc()));
                } catch (SSLException e) {
                    this.client.getExceptionListener().queue(new KittehConnectionException(e, true));
                    return;
                }
            }

            // Clean up on disconnect
            this.channel.closeFuture().addListener(futureListener -> {
                if (ClientConnection.this.reconnect) {
                    ClientConnection.this.channel.eventLoop().schedule(ClientConnection.this.client::connect, 5, TimeUnit.SECONDS);
                }
                ClientConnection.this.client.getEventManager().callEvent(new ClientConnectionClosedEvent(ClientConnection.this.client, ClientConnection.this.reconnect));
                removeClientConnection(ClientConnection.this, ClientConnection.this.reconnect);
            });
        }

        void sendMessage(String message, boolean priority) {
            if (priority) {
                this.channel.writeAndFlush(message);
            } else {
                this.queue.add(message);
            }
        }

        void shutdown(String message) {
            this.shutdown(message, false);
        }

        void startSending() {
            this.schedule(true);
        }

        void updateScheduling() {
            this.schedule(false);
        }

        private void schedule(boolean force) {
            synchronized (this.scheduledSendingLock) {
                if (!force && this.scheduledSending == null) {
                    return;
                }
                long delay = 0;
                if (this.scheduledSending != null) {
                    delay = this.scheduledSending.getDelay(TimeUnit.MILLISECONDS); // Negligible added delay processing this
                    this.scheduledSending.cancel(false);
                }
                this.scheduledSending = this.channel.eventLoop().scheduleAtFixedRate(() -> {
                    String message = ClientConnection.this.queue.poll();
                    if (message != null) {
                        ClientConnection.this.channel.writeAndFlush(message);
                    }
                }, delay, this.client.getMessageDelay(), TimeUnit.MILLISECONDS);
            }
        }

        private void shutdown(String message, boolean reconnect) {
            this.reconnect = reconnect;

            final StringBuilder quitBuilder = new StringBuilder();
            quitBuilder.append("QUIT");
            if (message != null) {
                quitBuilder.append(" :").append(message);
            }
            final String quitMessage = quitBuilder.toString();

            this.sendMessage(quitMessage, true);
            this.channel.close();
        }
    }

    private static final Bootstrap bootstrap = new Bootstrap();
    private static EventLoopGroup eventLoopGroup = null;
    private static final Set<ClientConnection> connections = new HashSet<>();

    static {
        bootstrap.channel(NioSocketChannel.class);
        bootstrap.handler(new ChannelInitializer<SocketChannel>() {
            @Override
            public void initChannel(SocketChannel channel) throws Exception {
                // NOOP
            }
        });
        bootstrap.option(ChannelOption.TCP_NODELAY, true);
    }

    private static synchronized void removeClientConnection(ClientConnection connection, boolean reconnecting) {
        connections.remove(connection);
        if (!reconnecting && connections.isEmpty()) {
            eventLoopGroup.shutdownGracefully();
            eventLoopGroup = null;
        }
    }

    synchronized static ClientConnection connect(IRCClient client) {
        if (eventLoopGroup == null) {
            eventLoopGroup = new NioEventLoopGroup();
            bootstrap.group(eventLoopGroup);
        }
        SocketAddress bind = client.getConfig().get(Config.BIND_ADDRESS);
        SocketAddress server = client.getConfig().get(Config.SERVER_ADDRESS);
        ClientConnection clientConnection;
        if (bind == null) {
            clientConnection = new ClientConnection(client, bootstrap.connect(server));
        } else {
            clientConnection = new ClientConnection(client, bootstrap.connect(server, bind));
        }
        connections.add(clientConnection);
        return clientConnection;
    }
}