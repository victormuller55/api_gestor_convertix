package br.net.convertix.gestor.entity;

import br.net.convertix.gestor.enums.EtapaProjeto;
import br.net.convertix.gestor.enums.OrigemAlteracaoStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "historico_etapa_projetos",
        indexes = {
                @Index(name = "idx_historico_etapa_projetos_projeto", columnList = "projeto_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HistoricoEtapaProjeto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "projeto_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_historico_etapa_projetos_projeto")
    )
    private Projeto projeto;

    @Enumerated(EnumType.STRING)
    @Column(name = "etapa_anterior", length = 50)
    private EtapaProjeto etapaAnterior;

    @Enumerated(EnumType.STRING)
    @Column(name = "etapa_nova", nullable = false, length = 50)
    private EtapaProjeto etapaNova;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private OrigemAlteracaoStatus origem;

    @Column(length = 500)
    private String mensagem;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
