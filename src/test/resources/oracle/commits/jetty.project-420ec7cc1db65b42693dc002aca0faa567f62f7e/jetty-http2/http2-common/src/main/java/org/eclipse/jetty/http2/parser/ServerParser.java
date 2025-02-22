//
// ========================================================================
// Copyright (c) 1995 Mort Bay Consulting Pty Ltd and others.
//
// This program and the accompanying materials are made available under the
// terms of the Eclipse Public License v. 2.0 which is available at
// https://www.eclipse.org/legal/epl-2.0, or the Apache License, Version 2.0
// which is available at https://www.apache.org/licenses/LICENSE-2.0.
//
// SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
// ========================================================================
//

package org.eclipse.jetty.http2.parser;

import java.nio.ByteBuffer;

import org.eclipse.jetty.http2.ErrorCode;
import org.eclipse.jetty.http2.Flags;
import org.eclipse.jetty.http2.frames.FrameType;
import org.eclipse.jetty.io.ByteBufferPool;
import org.eclipse.jetty.util.BufferUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServerParser extends Parser
{
    private static final Logger LOG = LoggerFactory.getLogger(ServerParser.class);

    private PrefaceParser prefaceParser;
    private State state = State.PREFACE;
    private boolean notifyPreface = true;

    @Deprecated
    public ServerParser(ByteBufferPool byteBufferPool, int maxTableSize, int maxHeaderSize, RateControl rateControl)
    {
        this(byteBufferPool, maxHeaderSize, rateControl);
    }

    public ServerParser(ByteBufferPool byteBufferPool, int maxHeaderSize, RateControl rateControl)
    {
        super(byteBufferPool, maxHeaderSize, rateControl);
    }

    @Override
    public void init(Parser.Listener listener)
    {
        super.init(listener);
        this.prefaceParser = new PrefaceParser(listener);
    }

    @Override
    protected Listener getListener()
    {
        return (Listener)super.getListener();
    }

    /**
     * <p>A direct upgrade is an unofficial upgrade from HTTP/1.1 to HTTP/2.0.</p>
     * <p>A direct upgrade is initiated when {@code org.eclipse.jetty.server.HttpConnection}
     * sees a request with these bytes:</p>
     * <pre>
     * PRI * HTTP/2.0\r\n
     * \r\n
     * </pre>
     * <p>This request is part of the HTTP/2.0 preface, indicating that a
     * HTTP/2.0 client is attempting a h2c direct connection.</p>
     * <p>This is not a standard HTTP/1.1 Upgrade path.</p>
     */
    public void directUpgrade()
    {
        if (state != State.PREFACE)
            throw new IllegalStateException();
        prefaceParser.directUpgrade();
    }

    /**
     * <p>The standard HTTP/1.1 upgrade path.</p>
     */
    public void standardUpgrade()
    {
        if (state != State.PREFACE)
            throw new IllegalStateException();
        notifyPreface = false;
    }

    @Override
    public void parse(ByteBuffer buffer)
    {
        try
        {
            if (LOG.isDebugEnabled())
                LOG.debug("Parsing {}", buffer);

            while (true)
            {
                switch (state)
                {
                    case PREFACE:
                    {
                        if (!prefaceParser.parse(buffer))
                            return;
                        if (notifyPreface)
                            onPreface();
                        state = State.SETTINGS;
                        break;
                    }
                    case SETTINGS:
                    {
                        if (!parseHeader(buffer))
                            return;
                        if (getFrameType() != FrameType.SETTINGS.getType() || hasFlag(Flags.ACK))
                        {
                            BufferUtil.clear(buffer);
                            notifyConnectionFailure(ErrorCode.PROTOCOL_ERROR.code, "invalid_preface");
                            return;
                        }
                        if (!parseBody(buffer))
                            return;
                        state = State.FRAMES;
                        break;
                    }
                    case FRAMES:
                    {
                        // Stay forever in the FRAMES state.
                        super.parse(buffer);
                        return;
                    }
                    default:
                    {
                        throw new IllegalStateException();
                    }
                }
            }
        }
        catch (Throwable x)
        {
            LOG.debug("Parse error", x);
            BufferUtil.clear(buffer);
            notifyConnectionFailure(ErrorCode.PROTOCOL_ERROR.code, "parser_error");
        }
    }

    protected void onPreface()
    {
        notifyPreface();
    }

    private void notifyPreface()
    {
        Listener listener = getListener();
        try
        {
            listener.onPreface();
        }
        catch (Throwable x)
        {
            LOG.info("Failure while notifying listener {}", listener, x);
        }
    }

    public interface Listener extends Parser.Listener
    {
        public void onPreface();

        public static class Adapter extends Parser.Listener.Adapter implements Listener
        {
            @Override
            public void onPreface()
            {
            }
        }

        public static class Wrapper extends Parser.Listener.Wrapper implements Listener
        {
            public Wrapper(ServerParser.Listener listener)
            {
                super(listener);
            }

            @Override
            public ServerParser.Listener getParserListener()
            {
                return (Listener)super.getParserListener();
            }

            @Override
            public void onPreface()
            {
                getParserListener().onPreface();
            }
        }
    }

    private enum State
    {
        PREFACE, SETTINGS, FRAMES
    }
}
