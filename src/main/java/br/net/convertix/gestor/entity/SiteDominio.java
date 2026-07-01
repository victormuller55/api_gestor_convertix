package br.net.convertix.gestor.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "site_dominio")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SiteDominio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "site_id", nullable = false, unique = true)
    private Site site;

    @Column(name = "valor_dominio", precision = 10, scale = 2)
    private BigDecimal valorDominio;

    @Column(name = "data_compra_dominio")
    private LocalDate dataCompraDominio;

    @Column(name = "data_fim_dominio")
    private LocalDate dataFimDominio;

    @Column(name = "data_renovacao")
    private LocalDate dataRenovacao;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
