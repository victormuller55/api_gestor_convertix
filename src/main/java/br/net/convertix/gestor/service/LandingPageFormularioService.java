package br.net.convertix.gestor.service;

import br.net.convertix.gestor.dto.request.LandingPageCampoRequest;
import br.net.convertix.gestor.dto.request.LandingPageFormularioRequest;
import br.net.convertix.gestor.dto.response.LandingPageCampoResponse;
import br.net.convertix.gestor.dto.response.LandingPageFormularioResponse;
import br.net.convertix.gestor.entity.LandingPage;
import br.net.convertix.gestor.entity.LandingPageCampo;
import br.net.convertix.gestor.entity.LandingPageFormulario;
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

import java.util.List;

@Service
@RequiredArgsConstructor
public class LandingPageFormularioService {

    private final LandingPageRepository landingPageRepository;
    private final LandingPageFormularioRepository formularioRepository;
    private final LandingPageCampoRepository campoRepository;
    private final LandingPageLeadRepository leadRepository;
    private final LandingPageLeadValorRepository leadValorRepository;
    private final AutorizacaoService autorizacaoService;

    @Transactional(readOnly = true)
    public List<LandingPageFormularioResponse> listarPorLandingPage(Long landingPageId) {
        validarLandingPage(landingPageId);
        return formularioRepository.findByLandingPageIdOrderByNomeAsc(landingPageId).stream()
                .map(MapperUtil::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public LandingPageFormularioResponse buscarPorId(Long landingPageId, Long formularioId) {
        validarLandingPage(landingPageId);
        return MapperUtil.toResponse(buscarFormularioDaLandingPage(landingPageId, formularioId));
    }

    @Transactional
    public LandingPageFormularioResponse criar(Long landingPageId, LandingPageFormularioRequest request) {
        LandingPage landingPage = validarLandingPage(landingPageId);

        LandingPageFormulario formulario = MapperUtil.toEntity(request, landingPage);
        return MapperUtil.toResponse(formularioRepository.save(formulario));
    }

    @Transactional
    public LandingPageFormularioResponse editar(
            Long landingPageId,
            Long formularioId,
            LandingPageFormularioRequest request) {
        validarLandingPage(landingPageId);
        LandingPageFormulario formulario = buscarFormularioDaLandingPage(landingPageId, formularioId);

        MapperUtil.updateEntity(formulario, request);
        return MapperUtil.toResponse(formularioRepository.save(formulario));
    }

    @Transactional
    public void excluir(Long landingPageId, Long formularioId) {
        validarLandingPage(landingPageId);
        LandingPageFormulario formulario = buscarFormularioDaLandingPage(landingPageId, formularioId);
        excluirLeadsDoFormulario(formularioId);
        formularioRepository.delete(formulario);
    }

    @Transactional(readOnly = true)
    public List<LandingPageCampoResponse> listarCampos(Long formularioId) {
        LandingPageFormulario formulario = buscarFormulario(formularioId);
        autorizacaoService.validarAcessoLandingPage(formulario.getLandingPage().getId());

        return campoRepository.findByFormularioIdOrderByOrdemAsc(formularioId).stream()
                .map(MapperUtil::toResponse)
                .toList();
    }

    @Transactional
    public LandingPageCampoResponse adicionarCampo(Long formularioId, LandingPageCampoRequest request) {
        LandingPageFormulario formulario = buscarFormulario(formularioId);
        autorizacaoService.validarAcessoLandingPage(formulario.getLandingPage().getId());

        validarNomeInternoUnico(formularioId, request.getNomeInterno(), null);

        LandingPageCampo campo = MapperUtil.toEntity(request, formulario);
        return MapperUtil.toResponse(campoRepository.save(campo));
    }

    @Transactional
    public LandingPageCampoResponse editarCampo(Long campoId, LandingPageCampoRequest request) {
        LandingPageCampo campo = buscarCampo(campoId);
        autorizacaoService.validarAcessoLandingPage(campo.getFormulario().getLandingPage().getId());

        validarNomeInternoUnico(campo.getFormulario().getId(), request.getNomeInterno(), campoId);

        MapperUtil.updateEntity(campo, request);
        return MapperUtil.toResponse(campoRepository.save(campo));
    }

    @Transactional
    public void removerCampo(Long campoId) {
        LandingPageCampo campo = buscarCampo(campoId);
        autorizacaoService.validarAcessoLandingPage(campo.getFormulario().getLandingPage().getId());
        leadValorRepository.findByCampoId(campoId).forEach(leadValorRepository::delete);
        campoRepository.delete(campo);
    }

    private void excluirLeadsDoFormulario(Long formularioId) {
        leadRepository.findByFormularioIdOrderByCreatedAtDesc(formularioId)
                .forEach(leadRepository::delete);
    }

    private LandingPage validarLandingPage(Long landingPageId) {
        if (landingPageId == null) {
            throw new BusinessException("A Landing Page é obrigatória");
        }

        autorizacaoService.validarAcessoLandingPage(landingPageId);

        return landingPageRepository.findById(landingPageId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Landing Page não encontrada com id: " + landingPageId));
    }

    private LandingPageFormulario buscarFormularioDaLandingPage(Long landingPageId, Long formularioId) {
        return formularioRepository.findByIdAndLandingPageId(formularioId, landingPageId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Formulário não encontrado com id: " + formularioId
                                + " para a Landing Page: " + landingPageId));
    }

    private LandingPageFormulario buscarFormulario(Long formularioId) {
        if (formularioId == null) {
            throw new BusinessException("O formulário é obrigatório");
        }

        return formularioRepository.findById(formularioId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Formulário não encontrado com id: " + formularioId));
    }

    private LandingPageCampo buscarCampo(Long campoId) {
        if (campoId == null) {
            throw new BusinessException("O campo é obrigatório");
        }

        return campoRepository.findById(campoId)
                .orElseThrow(() -> new ResourceNotFoundException("Campo não encontrado com id: " + campoId));
    }

    private void validarNomeInternoUnico(Long formularioId, String nomeInterno, Long campoIdAtual) {
        campoRepository.findByFormularioIdAndNomeInterno(formularioId, nomeInterno)
                .ifPresent(campoExistente -> {
                    if (campoIdAtual == null || !campoExistente.getId().equals(campoIdAtual)) {
                        throw new BusinessException("Já existe um campo com o nome interno: " + nomeInterno);
                    }
                });
    }
}
