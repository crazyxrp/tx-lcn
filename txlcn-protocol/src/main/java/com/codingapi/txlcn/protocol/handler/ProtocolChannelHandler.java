package com.codingapi.txlcn.protocol.handler;

import com.codingapi.txlcn.protocol.Protocoler;
import com.codingapi.txlcn.protocol.message.Connection;
import com.codingapi.txlcn.protocol.message.Heartbeat;
import com.codingapi.txlcn.protocol.message.Message;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;

/**
 * @author lorne
 * @date 2020/3/4
 * @description
 */
@Slf4j
@ChannelHandler.Sharable
public class ProtocolChannelHandler  extends SimpleChannelInboundHandler<Message> {

    private final Protocoler protocoler;

    private static final String SESSION_ATTRIBUTE_KEY = "session";

    @Getter
    private final ApplicationContext applicationContext;

    public ProtocolChannelHandler(Protocoler protocoler,ApplicationContext applicationContext) {
        this.protocoler = protocoler;
        this.applicationContext = applicationContext;
    }

    static Attribute<Connection> getSessionAttribute(ChannelHandlerContext ctx) {
        return ctx.channel().attr(AttributeKey.<Connection>valueOf(SESSION_ATTRIBUTE_KEY));
    }


    @Override
    public void channelActive(final ChannelHandlerContext ctx) throws Exception {
        log.debug("Channel active {}", ctx.channel().remoteAddress());
        final Connection connection = new Connection(ctx);
        getSessionAttribute(ctx).set(connection);
        protocoler.handleConnectionOpened(connection);
    }

    @Override
    public void channelInactive(final ChannelHandlerContext ctx) throws Exception {
        log.debug("Channel inactive {}", ctx.channel().remoteAddress());
        final Connection connection = getSessionAttribute(ctx).get();
        protocoler.handleConnectionClosed(connection);
    }


    @Override
    public void channelRead0(final ChannelHandlerContext ctx, final Message message) throws Exception {
        log.debug("Message {} received from {}", message.getClass(), ctx.channel().remoteAddress());
        final Connection connection = getSessionAttribute(ctx).get();
        message.handle(applicationContext,protocoler, connection);
    }


    @Override
    public void userEventTriggered(final ChannelHandlerContext ctx, final Object evt) {
        if (evt instanceof IdleStateEvent) {
            final IdleStateEvent idleStateEvent = (IdleStateEvent) evt;
            if (idleStateEvent.state() == IdleState.READER_IDLE) {
                log.warn("Channel idle {}", ctx.channel().remoteAddress());
                ctx.close();
            }

            if(idleStateEvent.state() == IdleState.WRITER_IDLE){
                log.debug("send heart message to {} ", ctx.channel().remoteAddress());
                ctx.writeAndFlush(new Heartbeat());
            }
        }
    }

}
