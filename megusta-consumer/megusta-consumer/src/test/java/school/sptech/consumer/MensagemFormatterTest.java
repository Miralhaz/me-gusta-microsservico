package school.sptech.consumer;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import school.sptech.megusta.notificacao.AlertaEstoque;
import school.sptech.megusta.notificacao.AlertaEstoqueMensagem;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("Testes da classe MensagemFormatter")
class MensagemFormatterTest {

    @Test
    @DisplayName("deve aceitar e normalizar um e-mail válido")
    void deveAceitarEmailValido() {
        assertEquals("responsavel@megusta.com", MensagemFormatter.email("  responsavel@megusta.com  "));
    }

    @Test
    @DisplayName("deve rejeitar e-mail nulo ou em branco")
    void deveRejeitarEmailNuloOuEmBranco() {
        assertThrows(IllegalArgumentException.class, () -> MensagemFormatter.email(null));
        assertThrows(IllegalArgumentException.class, () -> MensagemFormatter.email(""));
        assertThrows(IllegalArgumentException.class, () -> MensagemFormatter.email("   "));
    }

    @Test
    @DisplayName("deve rejeitar e-mail maior que 254 caracteres")
    void deveRejeitarEmailGrande() {
        String local = "a".repeat(250);
        assertThrows(IllegalArgumentException.class,
                () -> MensagemFormatter.email(local + "@megusta.com"));
    }

    @Test
    @DisplayName("deve rejeitar e-mail com formato inválido")
    void deveRejeitarEmailMalformado() {
        assertThrows(IllegalArgumentException.class, () -> MensagemFormatter.email("sem-arroba"));
        assertThrows(IllegalArgumentException.class, () -> MensagemFormatter.email("a@b"));
        assertThrows(IllegalArgumentException.class, () -> MensagemFormatter.email("a b@megusta.com"));
        assertThrows(IllegalArgumentException.class, () -> MensagemFormatter.email("@megusta.com"));
    }

    @Test
    @DisplayName("deve formatar o resumo com todos os insumos e vírgula decimal")
    void deveFormatarResumo() {
        AlertaEstoqueMensagem mensagem = new AlertaEstoqueMensagem(
                "responsavel@megusta.com",
                List.of(
                        new AlertaEstoque("Farinha", "INS001", 2.0, 5.0),
                        new AlertaEstoque("Queijo mussarela", "INS002", 1.5, 4.0)
                )
        );

        assertEquals(
                "Farinha (INS001): atual 2, mínimo 5; Queijo mussarela (INS002): atual 1,5, mínimo 4",
                MensagemFormatter.resumo(mensagem)
        );
    }

    @Test
    @DisplayName("deve rejeitar alertas vazios ou inválidos")
    void deveRejeitarAlertasInvalidos() {
        assertThrows(IllegalArgumentException.class,
                () -> MensagemFormatter.resumo(new AlertaEstoqueMensagem("responsavel@megusta.com", List.of())));
        assertThrows(IllegalArgumentException.class,
                () -> MensagemFormatter.resumo(new AlertaEstoqueMensagem(
                        "responsavel@megusta.com",
                        List.of(new AlertaEstoque("Farinha", "INS001", -1.0, 5.0)))));
    }

    @Test
    @DisplayName("deve rejeitar resumo maior que 800 caracteres")
    void deveRejeitarResumoGrande() {
        AlertaEstoqueMensagem mensagem = new AlertaEstoqueMensagem(
                "responsavel@megusta.com",
                List.of(new AlertaEstoque("a".repeat(800), "INS001", 2.0, 5.0))
        );

        assertThrows(IllegalArgumentException.class, () -> MensagemFormatter.resumo(mensagem));
    }
}
