package school.sptech.consumer;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Valida a configuração SMTP no início da aplicação.
 *
 * <p>Falhar cedo evita que o consumer suba sem conseguir entregar alertas e só
 * descubra o problema quando a primeira mensagem chegar.</p>
 */
@Component
public class MailConfigurationValidator {

    public MailConfigurationValidator(
            @Value("${spring.mail.host:}") String host,
            @Value("${spring.mail.port:0}") int port,
            @Value("${spring.mail.username:}") String username,
            @Value("${spring.mail.password:}") String password,
            @Value("${megusta.mail.from:}") String from) {
        if (host == null || host.isBlank()) {
            throw new IllegalStateException("Configure MAIL_HOST com o host SMTP.");
        }
        if (port < 1 || port > 65535) {
            throw new IllegalStateException("Configure MAIL_PORT com uma porta SMTP válida (1-65535).");
        }
        if (username == null || username.isBlank()) {
            throw new IllegalStateException("Configure MAIL_USERNAME com o usuário SMTP.");
        }
        if (password == null || password.isBlank()) {
            throw new IllegalStateException("Configure MAIL_PASSWORD com a senha SMTP.");
        }
        if (from == null || from.isBlank()) {
            throw new IllegalStateException("Configure MAIL_FROM com o e-mail remetente.");
        }
    }
}
