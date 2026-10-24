package school.sptech.megusta.notificacao;

import java.util.List;


public record AlertaEstoqueMensagem(
        String telefone,
        List<AlertaEstoque> alertas
) {
}
