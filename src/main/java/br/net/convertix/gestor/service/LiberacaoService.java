package br.net.convertix.gestor.service;

import br.net.convertix.gestor.entity.Assinatura;
import br.net.convertix.gestor.entity.Site;
import br.net.convertix.gestor.enums.StatusAssinatura;
import br.net.convertix.gestor.enums.StatusPagamento;
import br.net.convertix.gestor.enums.StatusSite;
import br.net.convertix.gestor.repository.AssinaturaRepository;
import br.net.convertix.gestor.repository.SiteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class LiberacaoService {

    private final SiteRepository siteRepository;
    private final AssinaturaRepository assinaturaRepository;

    @Transactional
    public void processarStatusPagamento(StatusPagamento status, Site site, Assinatura assinatura) {
        if (status == StatusPagamento.RECEIVED || status == StatusPagamento.CONFIRMED) {
            liberar(site, assinatura);
            return;
        }

        if (status == StatusPagamento.REFUNDED
                || status == StatusPagamento.CANCELLED
                || status == StatusPagamento.DELETED
                || status == StatusPagamento.OVERDUE
                || status == StatusPagamento.FAILED) {
            avaliarBloqueio(site, assinatura);
        }
    }

    @Transactional
    public void processarStatusAssinatura(Assinatura assinatura) {
        if (assinatura == null) {
            return;
        }
        if (assinatura.getStatus() == StatusAssinatura.ACTIVE) {
            liberar(assinatura.getSite(), assinatura);
        } else {
            avaliarBloqueio(assinatura.getSite(), assinatura);
        }
    }

    private void liberar(Site site, Assinatura assinatura) {
        Site alvo = site != null ? site : (assinatura != null ? assinatura.getSite() : null);
        if (alvo == null) {
            return;
        }
        if (alvo.getStatus() != StatusSite.ATIVO) {
            alvo.setStatus(StatusSite.ATIVO);
            siteRepository.save(alvo);
            log.info("Site {} liberado automaticamente após pagamento/assinatura", alvo.getId());
        }
    }

    private void avaliarBloqueio(Site site, Assinatura assinatura) {
        Site alvo = site != null ? site : (assinatura != null ? assinatura.getSite() : null);
        if (alvo == null) {
            return;
        }

        Long clienteId = alvo.getCliente().getId();
        boolean possuiAssinaturaAtiva = assinaturaRepository.existsByClienteIdAndStatus(clienteId, StatusAssinatura.ACTIVE);
        if (possuiAssinaturaAtiva) {
            return;
        }

        if (alvo.getStatus() == StatusSite.ATIVO) {
            alvo.setStatus(StatusSite.INATIVO);
            siteRepository.save(alvo);
            log.info("Site {} bloqueado automaticamente por inadimplência/cancelamento", alvo.getId());
        }
    }
}
