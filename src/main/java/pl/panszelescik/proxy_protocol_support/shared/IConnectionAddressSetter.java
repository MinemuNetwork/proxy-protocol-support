package pl.panszelescik.proxy_protocol_support.shared;

import java.net.SocketAddress;

public interface IConnectionAddressSetter {

    void setProxyProtocolAddress(SocketAddress address);
}
