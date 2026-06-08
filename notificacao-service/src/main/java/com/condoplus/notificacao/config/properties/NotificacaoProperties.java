package com.condoplus.notificacao.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "condoplus.notificacao")
public class NotificacaoProperties {

    private Retry retry = new Retry();
    private Canais canais = new Canais();

    public Canais canais() {
        return this.canais;
    }

    public Retry retry() {
        return this.retry;
    }

    @Data
    public static class Retry {
        private int maxTentativas = 3;
        private int backoffSegundosInicial = 5;
    }

    @Data
    public static class Canais {
        private Email email = new Email();
        private Push push = new Push();
        private Whatsapp whatsapp = new Whatsapp();

        public Email email() { return this.email; }
        public Push push() { return this.push; }
        public Whatsapp whatsapp() { return this.whatsapp; }
    }

    @Data
    public static class Email {
        private boolean habilitado = true;
        private String host;
        private int port;

        public boolean habilitado() {
            return this.habilitado;
        }
    }

    @Data
    public static class Push {
        private boolean habilitado = false;

        public boolean habilitado() {
            return this.habilitado;
        }
    }

    @Data
    public static class Whatsapp {
        private boolean habilitado = false;

        public boolean habilitado() {
            return this.habilitado;
        }
    }
}
