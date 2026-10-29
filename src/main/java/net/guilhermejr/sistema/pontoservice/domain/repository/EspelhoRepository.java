package net.guilhermejr.sistema.pontoservice.domain.repository;

import net.guilhermejr.sistema.pontoservice.domain.entity.Espelho;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EspelhoRepository extends JpaRepository<Espelho, Long> {

    /** Os dias vêm junto: o resumo de cada mês é calculado sobre eles. */
    @EntityGraph(attributePaths = "dias")
    List<Espelho> findAllByUsuarioOrderByAnoAscMesAsc(UUID usuario);

    Optional<Espelho> findByUsuarioAndAnoAndMes(UUID usuario, Integer ano, Integer mes);

}
