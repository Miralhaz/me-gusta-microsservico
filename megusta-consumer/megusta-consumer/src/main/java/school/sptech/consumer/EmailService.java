package school.sptech.consumer;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Adapter de saída que entrega o alerta de estoque por SMTP.
 *
 * <p>O corpo é texto puro para evitar markup e injeção de HTML a partir dos
 * nomes de insumos. Falhas do SMTP sobem como exceção para acionar o retry e a
 * DLQ configurados no listener.</p>
 */
@Service
public class EmailService {

    static final String ASSUNTO = "Alerta de estoque MeGusta";

    private final JavaMailSender mailSender;
    private final String remetente;

    public EmailService(JavaMailSender mailSender, @Value("${megusta.mail.from}") String remetente) {
        this.mailSender = mailSender;
        this.remetente = remetente;
    }

    public void enviar(String email, String resumo) {
        SimpleMailMessage mensagem = new SimpleMailMessage();
        mensagem.setFrom(remetente);
        mensagem.setTo(email);
        mensagem.setSubject(ASSUNTO);
        mensagem.setText(resumo);

        mailSender.send(mensagem);
    }
}
