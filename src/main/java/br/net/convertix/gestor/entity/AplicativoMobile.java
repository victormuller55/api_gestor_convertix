package br.net.convertix.gestor.entity;

import br.net.convertix.gestor.enums.StatusAplicativoMobile;
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
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "aplicativos_mobile",
        indexes = {
                @Index(name = "idx_aplicativos_mobile_cliente", columnList = "cliente_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AplicativoMobile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "cliente_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_aplicativos_mobile_cliente")
    )
    private Cliente cliente;

    @Column(nullable = false, length = 150)
    private String nome;

    @Column(length = 500)
    private String descricao;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private StatusAplicativoMobile status;

    @Column(name = "package_android", length = 255)
    private String packageAndroid;

    @Column(name = "bundle_id_ios", length = 255)
    private String bundleIdIos;

    @Column(name = "versao_android", length = 50)
    private String versaoAndroid;

    @Column(name = "versao_ios", length = 50)
    private String versaoIos;

    @Column(name = "url_android", length = 500)
    private String urlAndroid;

    @Column(name = "url_ios", length = 500)
    private String urlIos;

    @Column(name = "icone_url", length = 500)
    private String iconeUrl;

    @Column(name = "documento_requisitos_url", length = 500)
    private String documentoRequisitosUrl;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
