package co.unicauca.bancopreguntas.question.api.dto;

import co.unicauca.bancopreguntas.question.domain.Pagina;
import co.unicauca.bancopreguntas.question.domain.Question;

import java.util.List;

/** Una página de preguntas. {@code pagina} cuenta desde 0, como el parámetro {@code page} de la consulta. */
public record QuestionPageResponse(List<QuestionResponse> contenido, int pagina, int tamano,
                                   long totalElementos, int totalPaginas) {

    public static QuestionPageResponse de(Pagina<Question> p) {
        return new QuestionPageResponse(
                p.elementos().stream().map(QuestionResponse::de).toList(),
                p.numero() - 1, p.tamano(), p.totalElementos(), p.totalPaginas());
    }
}
