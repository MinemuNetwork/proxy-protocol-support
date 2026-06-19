package pl.panszelescik.proxy_protocol_support.shared.impl;

import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.handler.codec.haproxy.HAProxyMessageDecoder;
import pl.panszelescik.proxy_protocol_support.shared.ProxyProtocolSupport;
import pl.panszelescik.proxy_protocol_support.shared.config.CIDRMatcher;

import java.net.InetAddress;
import java.net.InetSocketAddress;

/**
 * Initializes the connection pipeline with Proxy Protocol support.
 * Performs connection triage to decide whether to apply PROXY protocol
 * or reject unauthorized connections.
 *
 * @author PanSzelescik
 */
public class ProxyProtocolChannelInitializer extends ChannelInitializer {

    private final IChannelInitializer channelInitializer;

    public ProxyProtocolChannelInitializer(IChannelInitializer channelInitializer) {
        this.channelInitializer = channelInitializer;
    }

    @Override
    protected void initChannel(Channel channel) throws Exception {
        this.channelInitializer.invokeInitChannel(channel);

        if (!ProxyProtocolSupport.enableProxyProtocol) {
            return;
        }

        if (ProxyProtocolSupport.whitelistedIPs.isEmpty()) {
            ProxyProtocolSupport.debugLogger.accept("Empty whitelist - accepting connection with Proxy Protocol");
            channel.pipeline()
                    .addAfter("timeout", "haproxy-decoder", new HAProxyMessageDecoder())
                    .addAfter("haproxy-decoder", "haproxy-handler", new ProxyProtocolHandler());
            return;
        }

        final InetSocketAddress remoteAddress = (InetSocketAddress) channel.remoteAddress();
        final InetAddress remoteIp = remoteAddress.getAddress();

        for (CIDRMatcher matcher : ProxyProtocolSupport.whitelistedIPs) {
            if (matcher.matches(remoteIp)) {
                ProxyProtocolSupport.debugLogger.accept("Accepted connection from whitelisted IP: " + remoteIp);
                channel.pipeline()
                        .addAfter("timeout", "haproxy-decoder", new HAProxyMessageDecoder())
                        .addAfter("haproxy-decoder", "haproxy-handler", new ProxyProtocolHandler());
                return;
            }
        }

        ProxyProtocolSupport.warnLogger.accept("Blocked connection from non-whitelisted IP: " + remoteIp);
        channel.close();
    }
}
