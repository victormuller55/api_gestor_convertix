package br.net.convertix.gestor.entity;

import br.net.convertix.gestor.enums.StatusSite;
import br.net.convertix.gestor.enums.TipoSite;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "sites")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Site {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    @Column(nullable = false)
    private String nome;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoSite tipo;

    private String dominio;

    private String subdominio;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusSite status;

    @OneToOne(mappedBy = "site", fetch = FetchType.LAZY)
    private BioLink bioLink;

    @OneToOne(mappedBy = "site", fetch = FetchType.LAZY)
    private LandingPage landingPage;

    @OneToOne(mappedBy = "site", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private SiteDominio siteDominio;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
