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
package org.kitteh.irc.client.library.element;

import org.kitteh.irc.client.library.Client;

/**
 * Represents an {@link Actor} capable of recieving messages.
 */
public interface MessageReceiver extends Actor {
    /**
     * Gets the name used when sending messages.
     *
     * @return the name by which you can message this receiver
     */
    String getMessagingName();

    /**
     * Sends this actor a CTCP message.
     *
     * @param message the message to send
     * @see Client#sendCTCPMessage(MessageReceiver, String)
     */
    default void sendCTCPMessage(String message) {
        this.getClient().sendCTCPMessage(this, message);
    }

    /**
     * Sends this actor a message.
     *
     * @param message the message to send
     * @see Client#sendMessage(MessageReceiver, String)
     */
    default void sendMessage(String message) {
        this.getClient().sendMessage(this, message);
    }

    /**
     * Sends this actor a message
     *
     * @param message the message to send
     * @see Client#sendNotice(MessageReceiver, String)
     */
    default void sendNotice(String message) {
        this.getClient().sendNotice(this, message);
    }
}