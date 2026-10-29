package net.guilhermejr.sistema.pontoservice.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import net.guilhermejr.sistema.pontoservice.domain.enums.TipoAjuste;

import java.io.Serializable;
import java.time.LocalDateTime;

/** Uma marcação incluída ou descartada pelo RH, do rodapé do espelho. */
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@NoArgsConstructor
@Entity
@Table(name = "ajustes")
public class Ajuste implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "espelho_id")
    private Espelho espelho;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 1)
    private TipoAjuste tipo;

    @Column(nullable = false)
    private LocalDateTime dataHora;

    private String motivo;

}
