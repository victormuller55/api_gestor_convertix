package br.net.convertix.gestor.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
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
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "landing_pages", indexes = {
        @Index(name = "idx_landing_pages_slug", columnList = "slug", unique = true),
        @Index(name = "idx_landing_pages_site_id", columnList = "site_id", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LandingPage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "site_id", nullable = false, unique = true)
    private Site site;

    @Column(nullable = false, unique = true)
    private String slug;

    @OneToMany(mappedBy = "landingPage", fetch = FetchType.LAZY)
    @Builder.Default
    private List<LandingPageFormulario> formularios = new ArrayList<>();

    @OneToMany(mappedBy = "landingPage", fetch = FetchType.LAZY)
    @Builder.Default
    private List<LandingPageLead> leads = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
