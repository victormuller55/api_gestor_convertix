package br.net.convertix.gestor.entity;

import br.net.convertix.gestor.enums.EtapaProjeto;
import br.net.convertix.gestor.enums.TipoProjeto;
import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "projetos",
        indexes = {
                @Index(name = "idx_projetos_cliente", columnList = "cliente_id"),
                @Index(name = "idx_projetos_etapa", columnList = "etapa"),
                @Index(name = "idx_projetos_site", columnList = "site_id"),
                @Index(name = "idx_projetos_aplicativo_mobile", columnList = "aplicativo_mobile_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Projeto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "cliente_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_projetos_cliente")
    )
    private Cliente cliente;

    @Column(nullable = false, length = 150)
    private String titulo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private TipoProjeto tipo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private EtapaProjeto etapa;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "site_id",
            foreignKey = @ForeignKey(name = "fk_projetos_site")
    )
    private Site site;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "aplicativo_mobile_id",
            foreignKey = @ForeignKey(name = "fk_projetos_aplicativo_mobile")
    )
    private AplicativoMobile aplicativoMobile;

    private LocalDate prazo;

    @Column(length = 2000)
    private String descricao;

    @Column(name = "observacao_interna", length = 2000)
    private String observacaoInterna;

    @OneToMany(mappedBy = "projeto", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("createdAt ASC")
    @Builder.Default
    private List<HistoricoEtapaProjeto> historicoEtapa = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
