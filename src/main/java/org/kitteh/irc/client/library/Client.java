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

import org.kitteh.irc.client.library.element.Channel;
import org.kitteh.irc.client.library.element.MessageReceiver;
import org.kitteh.irc.client.library.event.client.ClientConnectedEvent;
import org.kitteh.irc.client.library.event.user.PrivateCTCPQueryEvent;

import java.util.Set;
import java.util.function.Consumer;

/**
 * An individual IRC connection, see {@link ClientBuilder} to create one.
 */
public interface Client {
    /**
     * Adds channels to this client.
     * <p>
     * Joins the channel if already connected.
     *
     * @param channel channel(s) to add
     */
    void addChannel(String... channel);

    /**
     * Adds channels to this client.
     * <p>
     * Joins the channel if already connected.
     *
     * @param channel channel(s) to add
     */
    void addChannel(Channel... channel);

    /**
     * Gets the named channel.
     *
     * @param name channel name
     * @return a channel snapshot of the named channel
     * @see #getChannels()
     */
    Channel getChannel(String name);

    /**
     * Gets the channels in which the client is currently present.
     *
     * @return the client's current channels
     */
    Set<Channel> getChannels();

    /**
     * Gets the client's event manager.
     *
     * @return the event manager for this client
     */
    EventManager getEventManager();

    /**
     * Gets the nickname the client intends to possess. May not reflect
     * the current nickname if it's taken. The client will automatically
     * attempt to take back this nickname.
     * <p>
     * Use {@link #getNick()} for the current nick.
     *
     * @return the nickname the client tries to maintain
     */
    String getIntendedNick();

    /**
     * Gets the delay between messages sent to the server.
     * <p>
     * Default is 1200ms.
     *
     * @return milliseconds between sent messages
     */
    int getMessageDelay();

    /**
     * Gets the client name. This name is just an internal name for reference
     * and is not visible from IRC.
     *
     * @return the client name
     */
    String getName();

    /**
     * Gets the current nickname the client has.
     *
     * @return the current nick
     */
    String getNick();

    /**
     * Gets information about the server to which the client is currently
     * connected. As long as the client remains connected the information
     * returned by this object will update according to information received
     * from the server. A new one can be acquired from {@link
     * ClientConnectedEvent}
     *
     * @return the server information object
     */
    ServerInfo getServerInfo();

    /**
     * Removes a channel from the client, leaving as necessary.
     *
     * @param channel channel to leave
     * @param reason part reason
     */
    void removeChannel(String channel, String reason);

    /**
     * Removes a channel from the client, leaving as necessary.
     *
     * @param channel channel to leave
     * @param reason part reason
     */
    void removeChannel(Channel channel, String reason);

    /**
     * Sends a CTCP message to a target user or channel. Automagically adds
     * the CTCP delimiter around the message and escapes the characters that
     * need escaping when sending a CTCP message.
     * <p>
     * <i>Note: CTCP replies should not be sent this way. Catch the message
     * with the {@link PrivateCTCPQueryEvent}</i>
     *
     * @param target the destination of the message
     * @param message the message to send
     */
    void sendCTCPMessage(String target, String message);

    /**
     * Sends a CTCP message to a target user or channel. Automagically adds
     * the CTCP delimiter around the message and escapes the characters that
     * need escaping when sending a CTCP message.
     * <p>
     * <i>Note: CTCP replies should not be sent this way. Catch the message
     * with the {@link PrivateCTCPQueryEvent}</i>
     *
     * @param target the destination of the message
     * @param message the message to send
     */
    void sendCTCPMessage(MessageReceiver target, String message);

    /**
     * Sends a message to a target user or channel.
     *
     * @param target the destination of the message
     * @param message the message to send
     */
    void sendMessage(String target, String message);

    /**
     * Sends a message to a target user or channel.
     *
     * @param target the destination of the message
     * @param message the message to send
     */
    void sendMessage(MessageReceiver target, String message);

    /**
     * Sends a notice to a target user or channel.
     *
     * @param target the destination of the message
     * @param message the message to send
     */
    void sendNotice(String target, String message);

    /**
     * Sends a notice to a target user or channel.
     *
     * @param target the destination of the message
     * @param message the message to send
     */
    void sendNotice(MessageReceiver target, String message);

    /**
     * Sends a raw IRC message.
     *
     * @param message message to send
     */
    void sendRawLine(String message);

    /**
     * Sends a raw IRC message, disregarding message delays and all sanity.
     * Live life on the wild side with this method designed to ensure you
     * get floodkicked before you finish dumping your life's work into chat.
     *
     * @param message message to send dangerously, you monster
     */
    void sendRawLineImmediately(String message);

    /**
     * Sets values for authentication with services on the server. The
     * client will not attempt to utilize them until a future reconnect.
     *
     * @param authType type of authentication (See {@link AuthType})
     * @param name username
     * @param pass password
     */
    void setAuth(AuthType authType, String name, String pass);

    /**
     * Sets a listener for all thrown exceptions on this client.
     * <p>
     * All exceptions are passed from a single, separate thread.
     *
     * @param listener catcher of throwables
     */
    void setExceptionListener(Consumer<Exception> listener);

    /**
     * Sets a listener for all incoming messages from the server.
     * <p>
     * All messages are passed from a single, separate thread.
     *
     * @param listener input listener
     */
    void setInputListener(Consumer<String> listener);

    /**
     * Sets the delay between messages sent to the server.
     * <p>
     * Default is 1200ms.
     *
     * @param delay milliseconds between sent messages
     */
    void setMessageDelay(int delay);

    /**
     * Sets the nick the client wishes to use.
     *
     * @param nick new nickname
     */
    void setNick(String nick);

    /**
     * Sets a listener for all outgoing messages to the server.
     * <p>
     * All messages are passed from a single, separate thread.
     *
     * @param listener output listener
     */
    void setOutputListener(Consumer<String> listener);

    /**
     * Shuts down the client.
     *
     * @param reason quit message to send, null for blank message
     */
    void shutdown(String reason);
}