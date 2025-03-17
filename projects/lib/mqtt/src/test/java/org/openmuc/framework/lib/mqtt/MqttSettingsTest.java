/*
 * Copyright 2011-2024 Fraunhofer ISE
 *
 * This file is part of OpenMUC.
 * For more information visit http://www.openmuc.org
 *
 * OpenMUC is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * OpenMUC is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with OpenMUC. If not, see <http://www.gnu.org/licenses/>.
 *
 */
package org.openmuc.framework.lib.mqtt;

import static org.openmuc.framework.lib.mqtt.MqttSettings.ProxySettings.PROXY_HTTP;
import static org.openmuc.framework.lib.mqtt.MqttSettings.ProxySettings.PROXY_HTTP_WITH_USER;
import static org.openmuc.framework.lib.mqtt.MqttSettings.ProxySettings.PROXY_SOCKS4;
import static org.openmuc.framework.lib.mqtt.MqttSettings.ProxySettings.PROXY_SOCKS5;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.hivemq.client.mqtt.MqttClientTransportConfigBuilder;
import com.hivemq.client.mqtt.MqttProxyConfig;
import com.hivemq.client.mqtt.MqttProxyProtocol;

public class MqttSettingsTest {

    @Test
    void noProxyConfigured() {
        Assertions.assertFalse(new ProxySettings("").hasSupportedProxyConfigured());
        Assertions.assertFalse(new ProxySettings(null).hasSupportedProxyConfigured());
    }

    @Test
    void proxySettingsValidWithUser() {
        MqttSettings settings = new ProxySettings("me:mutti123@localhost:123");
        Assertions.assertTrue(settings.hasSupportedProxyConfigured());
        Assertions.assertEquals("me", settings.getProxySettings().get().toMqttProxyConfig().getUsername().get());
        Assertions.assertEquals("mutti123", settings.getProxySettings().get().toMqttProxyConfig().getPassword().get());
    }

    @Test
    void proxySettingsValidHttpNoUser() {
        MqttSettings settings = new ProxySettings("localhost:123");
        Assertions.assertTrue(settings.hasSupportedProxyConfigured());
        Assertions.assertEquals("localhost",
                settings.getProxySettings().get().toMqttProxyConfig().getAddress().getHostName());
        Assertions.assertEquals(123, settings.getProxySettings().get().toMqttProxyConfig().getAddress().getPort());
        Assertions.assertEquals(MqttProxyProtocol.HTTP,
                settings.getProxySettings().get().toMqttProxyConfig().getProtocol());
        Assertions.assertEquals(Optional.empty(), settings.getProxySettings().get().toMqttProxyConfig().getUsername());
        Assertions.assertEquals(Optional.empty(), settings.getProxySettings().get().toMqttProxyConfig().getUsername());
    }

    @Test
    void proxySettingsValidSocks4() {
        MqttSettings settings = new ProxySettings("socks4://154.12.253.232:40616");
        Assertions.assertTrue(settings.hasSupportedProxyConfigured());
        Assertions.assertEquals("154.12.253.232",
                settings.getProxySettings().get().toMqttProxyConfig().getAddress().getHostString());
        Assertions.assertEquals(40616, settings.getProxySettings().get().toMqttProxyConfig().getAddress().getPort());
        Assertions.assertEquals(MqttProxyProtocol.SOCKS_4,
                settings.getProxySettings().get().toMqttProxyConfig().getProtocol());

        Assertions.assertTrue(settings.getProxySettings().get().toMqttProxyConfig().getPassword().isEmpty());
        Assertions.assertTrue(settings.getProxySettings().get().toMqttProxyConfig().getUsername().isEmpty());
    }

    @Test
    void proxySettingsValidSocks5() {
        MqttSettings settings = new ProxySettings("socks5://64.187.232.70:2277");
        Assertions.assertTrue(settings.hasSupportedProxyConfigured());
        Assertions.assertEquals("64.187.232.70",
                settings.getProxySettings().get().toMqttProxyConfig().getAddress().getHostString());
        Assertions.assertEquals(2277, settings.getProxySettings().get().toMqttProxyConfig().getAddress().getPort());
        Assertions.assertEquals(MqttProxyProtocol.SOCKS_5,
                settings.getProxySettings().get().toMqttProxyConfig().getProtocol());

        Assertions.assertTrue(settings.getProxySettings().get().toMqttProxyConfig().getPassword().isEmpty());
        Assertions.assertTrue(settings.getProxySettings().get().toMqttProxyConfig().getUsername().isEmpty());
    }

    @Test
    void proxySettingsValidNoUserHttp() {
        MqttSettings settings = new ProxySettings("http://1.2.3.4:5678");
        Assertions.assertTrue(settings.hasSupportedProxyConfigured());
        Assertions.assertEquals("1.2.3.4",
                settings.getProxySettings().get().toMqttProxyConfig().getAddress().getHostName());
        Assertions.assertEquals(5678, settings.getProxySettings().get().toMqttProxyConfig().getAddress().getPort());
    }

    @Test
    void validSettingsAreProperlyForwarded() {
        MqttClientTransportConfigBuilder mock = Mockito.mock(MqttClientTransportConfigBuilder.class);
        new ProxySettings("http://foo:bar@utini:42").applyProxy(mock);
        Mockito.verify(mock)
                .proxyConfig(MqttProxyConfig.builder()
                        .protocol(MqttProxyProtocol.HTTP)//
                        .host("utini")//
                        .port(42)//
                        .username("foo")//
                        .password("bar")//
                        .build());
    }

    @Test
    void noProxyAppliedIfNoConfigProvided() {
        MqttClientTransportConfigBuilder mock = Mockito.mock(MqttClientTransportConfigBuilder.class);
        String noProxyConfiguration = "";
        new ProxySettings(noProxyConfiguration).applyProxy(mock);
        Mockito.verifyNoInteractions(mock);
    }

    @Test
    void proxySettingsInvalidScheme() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> new ProxySettings("whatever://localhost:123"));
    }

    @Test
    void throwsOnBadConfig() {
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> new ProxySettings("this-is-invalid-proxy-config:123&$12"));
    }

    @Test
    void exampleProxiesWork() {
        List<String> validProxies = Arrays.asList(PROXY_SOCKS4, PROXY_SOCKS5, PROXY_HTTP, PROXY_HTTP_WITH_USER);

        for (String proxyConfig : validProxies) {
            proxyConfig = proxyConfig.replace("port", "123");
            MqttSettings settings = new ProxySettings(proxyConfig);
            Assertions.assertTrue(settings.hasSupportedProxyConfigured(), "Expecting valid proxy for " + proxyConfig);
        }
    }

    static class ProxySettings extends MqttSettings {
        public ProxySettings(String proxyString) {
            super("host-does-not-matter", 123, 0, "local-address-does-not-matter\"", "username", "password", false, 1,
                    2, 3, 4, 5, "./wherever", null, null, false, null, null, 6, 7, false, false, proxyString,
                    "openmuc-parser");
        }
    }
}
