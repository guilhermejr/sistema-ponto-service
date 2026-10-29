package net.guilhermejr.sistema.pontoservice.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.time.LocalDate;

/** Uma linha do espelho. Horas em minutos, com sinal; null é célula vazia (diferente de 00:00). */
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@NoArgsConstructor
@Entity
@Table(name = "dias")
public class Dia implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "espelho_id")
    private Espelho espelho;

    @Column(nullable = false)
    private LocalDate data;

    /** Pares entrada-saída como no espelho: "08:53-12:48 13:56-17:59". */
    private String marcacoes;

    private String ocorrencia;

    private Integer abono;

    private Integer trabalhadas;

    private Integer compensaveis;

    /** Saldo do banco de horas no fim do dia. Zera a cada semestre (janeiro e julho). */
    private Integer acumulo;

    private Integer naoCompensaveis;

}
