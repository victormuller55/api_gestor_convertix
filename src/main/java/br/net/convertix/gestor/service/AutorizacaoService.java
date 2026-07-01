package br.net.convertix.gestor.service;

import br.net.convertix.gestor.entity.BioLink;
import br.net.convertix.gestor.entity.LandingPage;
import br.net.convertix.gestor.entity.Site;
import br.net.convertix.gestor.exception.ResourceNotFoundException;
import br.net.convertix.gestor.repository.BioLinkRepository;
import br.net.convertix.gestor.repository.LandingPageRepository;
import br.net.convertix.gestor.repository.SiteRepository;
import br.net.convertix.gestor.security.SecurityUtil;
import br.net.convertix.gestor.security.UsuarioAutenticado;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AutorizacaoService {

    private final SiteRepository siteRepository;
    private final BioLinkRepository bioLinkRepository;
    private final LandingPageRepository landingPageRepository;

    public Long getClienteIdFiltro() {
        UsuarioAutenticado usuario = SecurityUtil.getUsuarioLogado();
        if (usuario.isAdmin()) {
            return null;
        }
        if (usuario.getClienteId() == null) {
            throw new AccessDeniedException("Usuário cliente sem empresa vinculada");
        }
        return usuario.getClienteId();
    }

    @Transactional(readOnly = true)
    public void validarAcessoSite(Long siteId) {
        if (SecurityUtil.getUsuarioLogado().isAdmin()) {
            return;
        }

        Site site = siteRepository.findById(siteId)
                .orElseThrow(() -> new ResourceNotFoundException("Site não encontrado com id: " + siteId));

        validarAcessoCliente(site.getCliente().getId());
    }

    @Transactional(readOnly = true)
    public void validarAcessoBioLink(Long bioLinkId) {
        if (SecurityUtil.getUsuarioLogado().isAdmin()) {
            return;
        }

        BioLink bioLink = bioLinkRepository.findById(bioLinkId)
                .orElseThrow(() -> new ResourceNotFoundException("BioLink não encontrado com id: " + bioLinkId));

        validarAcessoCliente(bioLink.getSite().getCliente().getId());
    }

    @Transactional(readOnly = true)
    public void validarAcessoLandingPage(Long landingPageId) {
        if (SecurityUtil.getUsuarioLogado().isAdmin()) {
            return;
        }

        LandingPage landingPage = landingPageRepository.findById(landingPageId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Landing Page não encontrada com id: " + landingPageId));

        validarAcessoCliente(landingPage.getSite().getCliente().getId());
    }

    public void validarAcessoCliente(Long clienteId) {
        UsuarioAutenticado usuario = SecurityUtil.getUsuarioLogado();

        if (usuario.isAdmin()) {
            return;
        }

        if (usuario.getClienteId() == null || !usuario.getClienteId().equals(clienteId)) {
            throw new AccessDeniedException("Acesso negado a este recurso");
        }
    }

    public void forcarClienteId(Long clienteIdInformado) {
        UsuarioAutenticado usuario = SecurityUtil.getUsuarioLogado();

        if (usuario.isAdmin()) {
            return;
        }

        if (clienteIdInformado != null && !clienteIdInformado.equals(usuario.getClienteId())) {
            throw new AccessDeniedException("Acesso negado a este cliente");
        }
    }

    public Long resolverClienteId(Long clienteIdInformado) {
        UsuarioAutenticado usuario = SecurityUtil.getUsuarioLogado();

        if (usuario.isAdmin()) {
            return clienteIdInformado;
        }

        if (usuario.getClienteId() == null) {
            throw new AccessDeniedException("Usuário cliente sem empresa vinculada");
        }

        return usuario.getClienteId();
    }
}
