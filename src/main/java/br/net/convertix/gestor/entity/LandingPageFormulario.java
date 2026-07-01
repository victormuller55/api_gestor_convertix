package br.net.convertix.gestor.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "landing_page_formularios", indexes = {
        @Index(name = "idx_landing_page_formularios_landing_page_id", columnList = "landing_page_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LandingPageFormulario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "landing_page_id", nullable = false)
    private LandingPage landingPage;

    @Column(nullable = false)
    private String nome;

    @Column(nullable = false)
    private String titulo;

    private String descricao;

    @Column(name = "texto_botao", nullable = false)
    private String textoBotao;

    @Column(nullable = false)
    private Boolean ativo;

    @OneToMany(mappedBy = "formulario", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<LandingPageCampo> campos = new ArrayList<>();

    @OneToMany(mappedBy = "formulario", fetch = FetchType.LAZY)
    @Builder.Default
    private List<LandingPageLead> leads = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
