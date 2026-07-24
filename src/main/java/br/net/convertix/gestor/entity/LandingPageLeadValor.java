package br.net.convertix.gestor.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
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

@Entity
@Table(name = "landing_page_lead_valores", indexes = {
        @Index(name = "idx_landing_page_lead_valores_lead_id", columnList = "lead_id"),
        @Index(name = "idx_landing_page_lead_valores_campo_id", columnList = "campo_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LandingPageLeadValor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lead_id", nullable = false)
    private LandingPageLead lead;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campo_id", nullable = false)
    private LandingPageCampo campo;

    @Column(nullable = false, length = 65535)
    private String valor;
}
