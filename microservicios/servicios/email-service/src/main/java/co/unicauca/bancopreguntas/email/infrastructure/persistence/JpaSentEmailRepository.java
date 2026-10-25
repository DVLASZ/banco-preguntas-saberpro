package co.unicauca.bancopreguntas.email.infrastructure.persistence;

import co.unicauca.bancopreguntas.email.application.port.SentEmailRepository;
import co.unicauca.bancopreguntas.email.domain.CorreoEnviado;
import co.unicauca.bancopreguntas.email.domain.EstadoCorreo;
import co.unicauca.bancopreguntas.email.domain.Pagina;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

/** Adaptador de persistencia del puerto {@link SentEmailRepository}. */
@Component
public class JpaSentEmailRepository implements SentEmailRepository {

    private final SentEmailJpaRepository repositorio;

    public JpaSentEmailRepository(SentEmailJpaRepository repositorio) {
        this.repositorio = repositorio;
    }

    @Override
    public void guardar(CorreoEnviado correo) {
        repositorio.saveAndFlush(SentEmailEntity.de(correo));
    }

    @Override
    public Pagina<CorreoEnviado> listar(EstadoCorreo estado, int pagina, int tamano) {
        PageRequest pedido = PageRequest.of(pagina - 1, tamano);
        Page<SentEmailEntity> resultado = estado == null
                ? repositorio.findByOrderByAttemptedAtDescIdAsc(pedido)
                : repositorio.findByStatusOrderByAttemptedAtDescIdAsc(estado.name(), pedido);
        return new Pagina<>(resultado.getContent().stream().map(SentEmailEntity::aDominio).toList(), pagina, tamano,
                (int) resultado.getTotalElements());
    }
}
