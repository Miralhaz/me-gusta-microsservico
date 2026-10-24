package school.sptech.megusta.notificacao;

import java.util.List;

/**
 * Contrato da mensagem publicada no RabbitMQ.
 *
 * <p>O {@code telefone} do destinatário fica no nível raiz e cada item de
 * {@code alertas} expõe {@code nome}, {@code codigoInsumo}, {@code qtdAtual} e
 * {@code estoqueMinimo}.</p>
 */
public record AlertaEstoqueMensagem(
        String telefone,
        List<AlertaEstoque> alertas
) {
}
