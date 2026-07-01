package br.net.convertix.gestor.service;

import br.net.convertix.gestor.dto.request.LandingPageLeadRequest;
import br.net.convertix.gestor.dto.request.LandingPageLeadStatusRequest;
import br.net.convertix.gestor.dto.response.LandingPageLeadResponse;
import br.net.convertix.gestor.entity.LandingPage;
import br.net.convertix.gestor.entity.LandingPageCampo;
import br.net.convertix.gestor.entity.LandingPageFormulario;
import br.net.convertix.gestor.entity.LandingPageLead;
import br.net.convertix.gestor.entity.LandingPageLeadValor;
import br.net.convertix.gestor.enums.StatusLandingPageLead;
import br.net.convertix.gestor.enums.StatusSite;
import br.net.convertix.gestor.enums.TipoSite;
import br.net.convertix.gestor.exception.BusinessException;
import br.net.convertix.gestor.exception.ResourceNotFoundException;
import br.net.convertix.gestor.repository.LandingPageCampoRepository;
import br.net.convertix.gestor.repository.LandingPageFormularioRepository;
import br.net.convertix.gestor.repository.LandingPageLeadRepository;
import br.net.convertix.gestor.repository.LandingPageLeadValorRepository;
import br.net.convertix.gestor.repository.LandingPageRepository;
import br.net.convertix.gestor.util.MapperUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class LandingPageLeadService {

    private static final String CHAVE_NOME = "nome";
    private static final String CHAVE_EMAIL = "email";
    private static final String CHAVE_TELEFONE = "telefone";

    private final LandingPageRepository landingPageRepository;
    private final LandingPageFormularioRepository formularioRepository;
    private final LandingPageCampoRepository campoRepository;
    private final LandingPageLeadRepository leadRepository;
    private final LandingPageLeadValorRepository leadValorRepository;
    private final AutorizacaoService autorizacaoService;

    @Transactional
    public LandingPageLeadResponse receberEnvio(
            String slug,
            LandingPageLeadRequest request,
            String ip,
            String userAgent,
            String origem) {
        LandingPage landingPage = buscarLandingPagePublicaPorSlug(slug);
        LandingPageFormulario formulario = buscarFormularioAtivo(landingPage, request.getFormularioId());

        Map<String, String> respostas = normalizarRespostas(request.getRespostas());
        List<LandingPageCampo> camposAtivos = campoRepository
                .findByFormularioIdAndAtivoTrueOrderByOrdemAsc(formulario.getId());

        validarCamposObrigatorios(camposAtivos, respostas);

        LandingPageLead lead = LandingPageLead.builder()
                .landingPage(landingPage)
                .formulario(formulario)
                .nome(respostas.get(CHAVE_NOME))
                .email(respostas.get(CHAVE_EMAIL))
                .telefone(respostas.get(CHAVE_TELEFONE))
                .ip(ip)
                .userAgent(userAgent)
                .origem(origem)
                .status(StatusLandingPageLead.NOVO)
                .build();

        lead = leadRepository.save(lead);
        salvarValores(lead, camposAtivos, respostas);

        return montarResponse(lead);
    }

    @Transactional(readOnly = true)
    public List<LandingPageLeadResponse> listarPorLandingPage(Long landingPageId) {
        validarAcessoLandingPage(landingPageId);
        return leadRepository.findByLandingPageIdOrderByCreatedAtDesc(landingPageId).stream()
                .map(this::montarResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public LandingPageLeadResponse buscarPorId(Long landingPageId, Long leadId) {
        validarAcessoLandingPage(landingPageId);
        LandingPageLead lead = buscarLeadDaLandingPage(landingPageId, leadId);
        return montarResponse(lead);
    }

    @Transactional
    public LandingPageLeadResponse alterarStatus(
            Long landingPageId,
            Long leadId,
            LandingPageLeadStatusRequest request) {
        validarAcessoLandingPage(landingPageId);
        LandingPageLead lead = buscarLeadDaLandingPage(landingPageId, leadId);

        lead.setStatus(request.getStatus());
        lead.setObservacao(request.getObservacao());

        return montarResponse(leadRepository.save(lead));
    }

    @Transactional
    public void excluir(Long landingPageId, Long leadId) {
        validarAcessoLandingPage(landingPageId);
        LandingPageLead lead = buscarLeadDaLandingPage(landingPageId, leadId);
        leadRepository.delete(lead);
    }

    private LandingPage buscarLandingPagePublicaPorSlug(String slug) {
        if (!StringUtils.hasText(slug)) {
            throw new BusinessException("O slug da Landing Page é obrigatório");
        }

        LandingPage landingPage = landingPageRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Landing Page não encontrada"));

        if (landingPage.getSite().getTipo() != TipoSite.LANDING_PAGE
                || landingPage.getSite().getStatus() != StatusSite.ATIVO) {
            throw new ResourceNotFoundException("Landing Page não encontrada");
        }

        return landingPage;
    }

    private LandingPageFormulario buscarFormularioAtivo(LandingPage landingPage, Long formularioId) {
        if (formularioId == null) {
            throw new BusinessException("O formulário é obrigatório");
        }

        LandingPageFormulario formulario = formularioRepository
                .findByIdAndLandingPageId(formularioId, landingPage.getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Formulário não encontrado com id: " + formularioId));

        if (!Boolean.TRUE.equals(formulario.getAtivo())) {
            throw new BusinessException("Formulário inativo");
        }

        return formulario;
    }

    private void validarAcessoLandingPage(Long landingPageId) {
        if (landingPageId == null) {
            throw new BusinessException("A Landing Page é obrigatória");
        }

        autorizacaoService.validarAcessoLandingPage(landingPageId);

        if (!landingPageRepository.existsById(landingPageId)) {
            throw new ResourceNotFoundException("Landing Page não encontrada com id: " + landingPageId);
        }
    }

    private LandingPageLead buscarLeadDaLandingPage(Long landingPageId, Long leadId) {
        return leadRepository.findByIdAndLandingPageId(leadId, landingPageId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Lead não encontrado com id: " + leadId + " para a Landing Page: " + landingPageId));
    }

    private Map<String, String> normalizarRespostas(Map<String, String> respostas) {
        if (respostas == null || respostas.isEmpty()) {
            throw new BusinessException("As respostas são obrigatórias");
        }

        Map<String, String> normalizadas = new HashMap<>();
        respostas.forEach((chave, valor) -> {
            if (StringUtils.hasText(chave) && valor != null) {
                normalizadas.put(chave.trim(), valor.trim());
            }
        });
        return normalizadas;
    }

    private void validarCamposObrigatorios(List<LandingPageCampo> campos, Map<String, String> respostas) {
        List<String> camposFaltantes = new ArrayList<>();

        for (LandingPageCampo campo : campos) {
            if (!Boolean.TRUE.equals(campo.getObrigatorio())) {
                continue;
            }

            String valor = respostas.get(campo.getNomeInterno());
            if (!StringUtils.hasText(valor)) {
                camposFaltantes.add(campo.getNomeInterno());
            }
        }

        if (!camposFaltantes.isEmpty()) {
            throw new BusinessException("Campos obrigatórios não preenchidos: " + String.join(", ", camposFaltantes));
        }
    }

    private void salvarValores(
            LandingPageLead lead,
            List<LandingPageCampo> campos,
            Map<String, String> respostas) {
        for (LandingPageCampo campo : campos) {
            String valor = respostas.get(campo.getNomeInterno());
            if (!StringUtils.hasText(valor)) {
                continue;
            }

            LandingPageLeadValor leadValor = LandingPageLeadValor.builder()
                    .lead(lead)
                    .campo(campo)
                    .valor(valor)
                    .build();

            leadValorRepository.save(leadValor);
        }
    }

    private LandingPageLeadResponse montarResponse(LandingPageLead lead) {
        List<LandingPageLeadValor> valores = leadValorRepository.findByLeadId(lead.getId());
        return MapperUtil.toResponse(lead, valores);
    }
}
