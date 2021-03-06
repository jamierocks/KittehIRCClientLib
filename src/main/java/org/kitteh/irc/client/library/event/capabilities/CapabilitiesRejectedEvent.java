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
package org.kitteh.irc.client.library.event.capabilities;

import org.kitteh.irc.client.library.CapabilityState;
import org.kitteh.irc.client.library.Client;
import org.kitteh.irc.client.library.event.CapabilityNegotiationResponseEvent;

import java.util.Collections;
import java.util.List;

/**
 * Fired when a CAP NAK is received.
 */
public class CapabilitiesRejectedEvent extends CapabilityNegotiationResponseEvent {
    private final List<CapabilityState> rejectedCapabilitiesRequest;

    public CapabilitiesRejectedEvent(Client client, boolean negotiating, List<CapabilityState> rejectedCapabilitiesRequest) {
        super(client, negotiating);
        this.rejectedCapabilitiesRequest = Collections.unmodifiableList(rejectedCapabilitiesRequest);
    }

    /**
     * Gets the rejected change, or at least the first 100 characters worth.
     *
     * @return rejected request list
     */
    public List<CapabilityState> getRejectedCapabilitiesRequest() {
        return this.rejectedCapabilitiesRequest;
    }
}