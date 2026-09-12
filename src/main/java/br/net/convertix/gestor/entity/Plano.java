package br.net.convertix.gestor.entity;

import br.net.convertix.gestor.enums.CicloAssinatura;
import br.net.convertix.gestor.enums.TipoProjeto;
import br.net.convertix.gestor.enums.VinculoPlano;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "planos",
        indexes = {
                @Index(name = "uk_planos_codigo", columnList = "codigo", unique = true),
                @Index(name = "idx_planos_ativo", columnList = "ativo")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Plano {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 50, unique = true)
    private String codigo;

    @Column(nullable = false, length = 150)
    private String nome;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private TipoProjeto tipo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private VinculoPlano vinculo;

    @Column(precision = 12, scale = 2)
    private BigDecimal valor;

    @Column(name = "valor_livre", nullable = false)
    private boolean valorLivre;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private CicloAssinatura ciclo;

    @Column(name = "descricao_padrao", length = 255)
    private String descricaoPadrao;

    @Column(nullable = false)
    private boolean ativo;

    @Column(nullable = false)
    private int ordem;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
