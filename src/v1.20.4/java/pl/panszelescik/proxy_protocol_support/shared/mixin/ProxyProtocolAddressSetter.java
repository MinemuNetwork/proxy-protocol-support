package pl.panszelescik.proxy_protocol_support.shared.mixin;

import net.minecraft.network.Connection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import pl.panszelescik.proxy_protocol_support.shared.IConnectionAddressSetter;

import java.net.SocketAddress;

/**
 * Adds Accessor for address field on Connection.
 * Implements the shared interface used by ProxyProtocolHandler.
 *
 * @author PanSzelescik
 * @see net.minecraft.network.Connection#address
 */
@Mixin(Connection.class)
public interface ProxyProtocolAddressSetter extends IConnectionAddressSetter {

    @Accessor("address")
    @Override
    void setProxyProtocolAddress(SocketAddress address);
}
