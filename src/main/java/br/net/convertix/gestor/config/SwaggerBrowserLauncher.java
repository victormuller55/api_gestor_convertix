package br.net.convertix.gestor.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.awt.Desktop;
import java.net.URI;

@Slf4j
@Component
@Profile("local")
public class SwaggerBrowserLauncher {

    @Value("${server.port:8080}")
    private int serverPort;

    @Value("${spring.application.swagger.auto-open:true}")
    private boolean autoOpen;

    @EventListener(ApplicationReadyEvent.class)
    public void abrirSwaggerNoNavegador() {
        if (!autoOpen) {
            return;
        }

        String url = "http://localhost:" + serverPort + "/swagger-ui.html";
        log.info("Abrindo Swagger UI em {}", url);

        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(new URI(url));
                return;
            }

            String os = System.getProperty("os.name").toLowerCase();
            if (os.contains("win")) {
                Runtime.getRuntime().exec(new String[]{"rundll32", "url.dll,FileProtocolHandler", url});
            } else if (os.contains("mac")) {
                Runtime.getRuntime().exec(new String[]{"open", url});
            } else {
                Runtime.getRuntime().exec(new String[]{"xdg-open", url});
            }
        } catch (Exception e) {
            log.warn("Não foi possível abrir o navegador automaticamente. Acesse manualmente: {}", url);
        }
    }
}
