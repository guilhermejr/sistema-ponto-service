package net.guilhermejr.sistema.pontoservice.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Um espelho de ponto mensal, como veio do PDF. Um por usuário e mês. */
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
@NoArgsConstructor
@Entity
@Table(name = "espelhos")
public class Espelho extends Auditoria implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(nullable = false)
    private Integer ano;

    @Column(nullable = false)
    private Integer mes;

    @Column(nullable = false)
    private LocalDate inicio;

    @Column(nullable = false)
    private LocalDate fim;

    @Column(nullable = false)
    private String horarioContratual;

    private String folhaPagamento;

    private String cargo;

    private String lotacao;

    private LocalDate emitido;

    private LocalDateTime assinado;

    /** Nome do arquivo enviado, só para referência. */
    private String arquivo;

    @OneToMany(mappedBy = "espelho", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("data")
    private List<Dia> dias = new ArrayList<>();

    @OneToMany(mappedBy = "espelho", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id")
    private List<Ajuste> ajustes = new ArrayList<>();

}
