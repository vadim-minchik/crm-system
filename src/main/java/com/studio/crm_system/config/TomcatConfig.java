package com.studio.crm_system.config;

import jakarta.servlet.MultipartConfigElement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.lang.reflect.Method;

@Configuration
public class TomcatConfig implements CommandLineRunner {

    private static final int  MAX_POST_MB    = 60;
    private static final long MAX_FILE_MB    = 25L;
    private static final long MAX_REQUEST_MB = 60L;

    @Autowired
    private ApplicationContext applicationContext;

    @Override
    public void run(String... args) {
        try {
            Method getWebServer = applicationContext.getClass().getMethod("getWebServer");
            Object webServer = getWebServer.invoke(applicationContext);

            Method getTomcat = webServer.getClass().getMethod("getTomcat");
            Object tomcat = getTomcat.invoke(webServer);

            Method getConnector = tomcat.getClass().getMethod("getConnector");
            Object connector = getConnector.invoke(tomcat);

            Method setMaxPostSize = connector.getClass().getMethod("setMaxPostSize", int.class);
            setMaxPostSize.invoke(connector, MAX_POST_MB * 1024 * 1024);

            System.out.printf("[TomcatConfig] ✓ maxPostSize = %d МБ%n", MAX_POST_MB);
        } catch (Exception e) {
            System.err.println("[TomcatConfig] ✗ Не удалось настроить maxPostSize: " + e);
        }
    }

    @Bean
    public MultipartConfigElement multipartConfigElement() {
        return new MultipartConfigElement(
                "",
                MAX_FILE_MB    * 1024 * 1024,
                MAX_REQUEST_MB * 1024 * 1024,
                0
        );
    }
}
