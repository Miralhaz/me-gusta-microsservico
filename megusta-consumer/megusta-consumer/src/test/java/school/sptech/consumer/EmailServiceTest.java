package school.sptech.consumer;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@DisplayName("Testes da classe EmailService")
class EmailServiceTest {

    private static final String REMETENTE = "alertas@megusta.com";

    @Test
    @DisplayName("deve enviar e-mail de texto puro com destinatário, assunto e corpo corretos")
    void deveEnviarEmail() {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        EmailService emailService = new EmailService(mailSender, REMETENTE);

        emailService.enviar("responsavel@megusta.com", "Farinha (INS001): atual 2, mínimo 5");

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());

        SimpleMailMessage mensagem = captor.getValue();
        assertEquals(REMETENTE, mensagem.getFrom());
        assertEquals("responsavel@megusta.com", mensagem.getTo()[0]);
        assertEquals("Alerta de estoque MeGusta", mensagem.getSubject());
        assertEquals("Farinha (INS001): atual 2, mínimo 5", mensagem.getText());
    }
}
