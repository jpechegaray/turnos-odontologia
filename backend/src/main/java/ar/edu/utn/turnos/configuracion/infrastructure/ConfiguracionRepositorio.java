package ar.edu.utn.turnos.configuracion.infrastructure;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ConfiguracionRepositorio extends JpaRepository<Configuracion, String> {

    @Query("select c.valor from Configuracion c where c.clave = :clave")
    Optional<String> consultarValor(@Param("clave") String clave);

    @Query("select c.clave as clave, c.valor as valor from Configuracion c")
    List<ValorVigente> consultarValores();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Configuracion c where c.clave in :claves order by c.clave")
    List<Configuracion> bloquearParaReemplazo(@Param("claves") List<String> claves);

    interface ValorVigente {
        String getClave();

        String getValor();
    }
}
