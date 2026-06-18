# Proxy Protocol Support

[Forge](https://files.minecraftforge.net/) mod que añade soporte para [Proxy Protocol (HAProxy)](https://www.haproxy.com/blog/haproxy/proxy-protocol/) en servidores Minecraft.

Permite usar [TCPShield](https://tcpshield.com/), [HAProxy](https://www.haproxy.org/), [Nginx](https://nginx.org/en/docs/stream/ngx_stream_proxy_module.html#proxy_protocol) u otros balanceadores que envíen el encabezado PROXY protocol, ocultando la IP real del servidor y mostrando la IP del jugador en logs y plugins.

**Solo servidor** — no hace nada en el cliente.

---

## Versiones soportadas

| Minecraft | Forge | Source set | Java |
|-----------|-------|------------|------|
| 1.19.4    | 45.0.49 | `v1.19.4` | 17 |
| 1.20.1    | 47.3.0 | `v1.20.1` | 17 |
| 1.20.4    | 49.0.50 | `v1.20.4` | 17 |
| 1.21      | 51.1.0 | `v1.21`   | 21 |

---

## Compilar

### 1.19.4 (default)

```bash
./gradlew build
```

El JAR se genera en `build/libs/proxy_protocol_support-<version>.jar`.

### Otras versiones

Cada versión se compila pasando propiedades por línea de comandos:

```bash
# 1.20.1
./gradlew wrapper --gradle-version 8.10
./gradlew build --no-daemon \
  -PminecraftVersion=1.20.1 \
  -PforgeVersion=47.3.0 \
  -PforgeGradleVersion=6.+ \
  -PversionSourceDir=v1.20.1

# 1.20.4
./gradlew build --no-daemon \
  -PminecraftVersion=1.20.4 \
  -PforgeVersion=49.0.50 \
  -PforgeGradleVersion=6.+ \
  -PversionSourceDir=v1.20.4

# 1.21
./gradlew build --no-daemon \
  -PminecraftVersion=1.21 \
  -PforgeVersion=51.1.0 \
  -PforgeGradleVersion=6.+ \
  -PjavaVersion=21 \
  -PversionSourceDir=v1.21
```

> **Nota:** 1.20+ requiere Gradle 8.x (ForgeGradle 6). 1.19.4 usa Gradle 7.x (ForgeGradle 5).
> El wrapper del repositorio está configurado para 1.19.4 por defecto.
> Para 1.21 se necesita Java 21.

---

## Configuración

El archivo de configuración se genera automáticamente en `config/proxy_protocol_support.json`:

```json
{
  "enable-proxy-protocol": true,
  "proxy-protocol-whitelisted-ips": ["127.0.0.1"],
  "whitelistTCPShieldServers": false
}
```

| Campo | Descripción |
|-------|-------------|
| `enable-proxy-protocol` | Activa/desactiva el mod |
| `proxy-protocol-whitelisted-ips` | Lista de IPs/CIDRs de proxies autorizados a enviar PROXY protocol |
| `whitelistTCPShieldServers` | Si `true`, obtiene automáticamente las IPs de TCPShield |

---

## Docker (entorno de pruebas)

El directorio `docker/` contiene un entorno completo para probar el mod con HAProxy + Forge:

```bash
./docker/run.sh
```

Esto compila el mod, copia el JAR y levanta:

- **HAProxy** → recibe conexiones en `:25565`, reenvía con `send-proxy-v2`
- **Forge 1.19.4** → recibe del proxy con el encabezado PROXY protocol

La configuración del mod en el contenedor usa `172.25.0.10` (IP fija del HAProxy) como whitelist.

```
Cliente ──→ HAProxy (:25565) ──→ Forge (proxy protocol v2)
```

---

## Estructura del proyecto

```
src/
├── main/java/          ← código compartido (todas las versiones)
│   └── pl/.../shared/
│       ├── config/     ← Config, CIDRMatcher, TCPShield
│       ├── impl/       ← ProxyProtocolChannelInitializer, ProxyProtocolHandler
│       ├── mixin/      ← ChannelInitializerInvoker (Netty, nunca cambia)
│       └── IConnectionAddressSetter.java
│
├── v1.19.4/java/       ← mixins específicos para 1.19.4
├── v1.20.1/java/       ← mixins para 1.20.1
├── v1.20.4/java/       ← mixins para 1.20.4
└── v1.21/java/         ← mixins para 1.21
```

---

## Cómo funciona

1. El mixin `ProxyProtocolImplementation` intercepta `ServerConnectionListener.startTcpServerListener()`
2. Envuelve el `ChannelInitializer` original de Minecraft con `ProxyProtocolChannelInitializer`
3. `ProxyProtocolChannelInitializer` inserta en el pipeline de Netty:
   - `HAProxyMessageDecoder` → decodifica el encabezado PROXY protocol
   - `ProxyProtocolHandler` → lee la IP/puerto real del jugador
4. `ProxyProtocolHandler` valida que la IP del proxy esté en la whitelist
5. Si es válido, el mixin `ProxyProtocolAddressSetter` escribe la IP real en `Connection#address`

---

## Licencia

MIT — ver archivo [LICENSE](LICENSE).
