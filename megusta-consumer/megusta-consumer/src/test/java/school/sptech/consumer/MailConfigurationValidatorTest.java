package school.sptech.consumer;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Testes da validação de configuração SMTP")
class MailConfigurationValidatorTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(MailConfigurationValidator.class)
            .withPropertyValues(
                    "spring.mail.host=smtp.example.com",
                    "spring.mail.port=587",
                    "spring.mail.username=usuario",
                    "spring.mail.password=segredo",
                    "megusta.mail.from=alertas@megusta.com");

    @Test
    @DisplayName("deve iniciar o contexto com a configuração completa")
    void deveIniciarComConfiguracaoCompleta() {
        runner.run(context -> assertThat(context).hasNotFailed());
    }

    @Test
    @DisplayName("deve falhar quando MAIL_FROM está ausente")
    void deveFalharSemRemetente() {
        runner.withPropertyValues("megusta.mail.from=").run(context -> {
            assertThat(context).hasFailed();
            assertThat(context.getStartupFailure()).hasRootCauseMessage(
                    "Configure MAIL_FROM com o e-mail remetente.");
        });
    }

    @Test
    @DisplayName("deve falhar quando MAIL_HOST está em branco")
    void deveFalharSemHost() {
        runner.withPropertyValues("spring.mail.host=").run(context -> {
            assertThat(context).hasFailed();
            assertThat(context.getStartupFailure()).hasRootCauseMessage(
                    "Configure MAIL_HOST com o host SMTP.");
        });
    }

    @Test
    @DisplayName("deve falhar quando MAIL_PORT é inválida")
    void deveFalharComPortaInvalida() {
        runner.withPropertyValues("spring.mail.port=70000").run(context -> {
            assertThat(context).hasFailed();
            assertThat(context.getStartupFailure()).hasRootCauseMessage(
                    "Configure MAIL_PORT com uma porta SMTP válida (1-65535).");
        });
    }
}
