package school.sptech.megusta.notificacao;

import java.util.List;


public record AlertaEstoqueMensagem(
        String email,
        List<AlertaEstoque> alertas
) {
}
