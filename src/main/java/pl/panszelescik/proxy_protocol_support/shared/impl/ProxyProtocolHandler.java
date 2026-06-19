package pl.panszelescik.proxy_protocol_support.shared.impl;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.haproxy.HAProxyCommand;
import io.netty.handler.codec.haproxy.HAProxyMessage;
import net.minecraft.network.Connection;
import pl.panszelescik.proxy_protocol_support.shared.IConnectionAddressSetter;
import pl.panszelescik.proxy_protocol_support.shared.ProxyProtocolSupport;

import java.net.InetSocketAddress;

/**
 * Reads HAProxyMessage to set the player's real IP address.
 * IP whitelist validation is handled by ProxyProtocolChannelInitializer.
 *
 * @author PanSzelescik
 * @see io.netty.handler.codec.haproxy.HAProxyMessage
 */
public class ProxyProtocolHandler extends ChannelInboundHandlerAdapter {

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof HAProxyMessage) {
            HAProxyMessage message = ((HAProxyMessage) msg);
            try {
                if (message.command() == HAProxyCommand.PROXY) {
                    final String realAddress = message.sourceAddress();
                    final int realPort = message.sourcePort();

                    if (realAddress == null) {
                        ProxyProtocolSupport.warnLogger.accept("Received PROXY header but source address was null. Closing connection.");
                        ctx.close();
                        return;
                    }

                    final InetSocketAddress socketAddr = new InetSocketAddress(realAddress, realPort);

                    Connection connection = ((Connection) ctx.channel().pipeline().get("packet_handler"));

                    ((IConnectionAddressSetter) (Object) connection).setProxyProtocolAddress(socketAddr);
                }
            } finally {
                message.release();
            }
        } else {
            super.channelRead(ctx, msg);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        ProxyProtocolSupport.warnLogger.accept("Connection without valid Proxy Protocol: " + ctx.channel().remoteAddress());
        ctx.close();
    }
}
