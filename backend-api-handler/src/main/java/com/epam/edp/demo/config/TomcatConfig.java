package com.epam.edp.demo.config;

import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configures embedded Tomcat to use Java's built-in JSSE for SSL handling.
 *
 * <p>Without this, Tomcat 10.1.x attempts to detect the native APR/OpenSSL library
 * via {@code org.apache.tomcat.jni.Library.initialize()}, which calls
 * {@code System::load} — a restricted method in Java's module system (JEP 472).
 * This produces the following JVM warnings on Java 17+:
 * <pre>
 *   WARNING: java.lang.System::load has been called by org.apache.tomcat.jni.Library
 *   WARNING: Use --enable-native-access=ALL-UNNAMED to avoid a warning
 * </pre>
 *
 * <p>By explicitly setting the SSL implementation to JSSE, Tomcat skips the native
 * library probe entirely, suppressing the warnings without any JVM flags.
 */
@Configuration
public class TomcatConfig {

    private static final String JSSE_IMPLEMENTATION =
            "org.apache.tomcat.util.net.jsse.JSSEImplementation";

    @Bean
    public WebServerFactoryCustomizer<TomcatServletWebServerFactory> disableTomcatNativeLibrary() {
        return factory -> factory.addConnectorCustomizers(connector ->
                // Force JSSE: prevents Tomcat from probing for native APR/OpenSSL library,
                // which causes JVM module-system warnings (JEP 472) on Java 17+
                connector.setProperty("sslImplementationName", JSSE_IMPLEMENTATION)
        );
    }
}

