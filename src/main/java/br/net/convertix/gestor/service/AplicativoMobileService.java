package br.net.convertix.gestor.service;

import br.net.convertix.gestor.dto.request.AplicativoMobileRequest;
import br.net.convertix.gestor.dto.response.AplicativoMobileResponse;
import br.net.convertix.gestor.dto.response.PageResponse;
import br.net.convertix.gestor.entity.AplicativoMobile;
import br.net.convertix.gestor.entity.Cliente;
import br.net.convertix.gestor.enums.StatusAplicativoMobile;
import br.net.convertix.gestor.exception.BusinessException;
import br.net.convertix.gestor.exception.ResourceNotFoundException;
import br.net.convertix.gestor.repository.AplicativoMobileRepository;
import br.net.convertix.gestor.repository.ClienteRepository;
import br.net.convertix.gestor.repository.spec.AplicativoMobileSpecification;
import br.net.convertix.gestor.security.SecurityUtil;
import br.net.convertix.gestor.util.MapperUtil;
import br.net.convertix.gestor.util.PaginationUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class AplicativoMobileService {

    private static final String PASTA_DOCUMENTOS = "aplicativos-mobile";

    private final AplicativoMobileRepository aplicativoMobileRepository;
    private final ClienteRepository clienteRepository;
    private final AutorizacaoService autorizacaoService;
    private final ArquivoService arquivoService;

    @Transactional(readOnly = true)
    public PageResponse<AplicativoMobileResponse> buscar(
            Long id,
            String query,
            Long clienteId,
            StatusAplicativoMobile status,
            int page,
            int size) {
        Long clienteIdFiltro = autorizacaoService.getClienteIdFiltro();
        if (clienteIdFiltro == null) {
            clienteIdFiltro = clienteId;
        } else {
            autorizacaoService.forcarClienteId(clienteId);
        }

        Page<AplicativoMobile> resultado = aplicativoMobileRepository.findAll(
                AplicativoMobileSpecification.comFiltros(id, query, clienteIdFiltro, status),
                PaginationUtil.of(page, size));
        return PaginationUtil.toResponse(resultado, MapperUtil::toResponse);
    }

    @Transactional(readOnly = true)
    public AplicativoMobileResponse buscarPorId(Long id) {
        return MapperUtil.toResponse(buscarEntidade(id));
    }

    @Transactional
    public AplicativoMobileResponse criar(AplicativoMobileRequest request) {
        SecurityUtil.exigirAdmin();
        Long clienteId = autorizacaoService.resolverClienteId(request.getClienteId());
        Cliente cliente = buscarCliente(clienteId);

        AplicativoMobile aplicativo = AplicativoMobile.builder()
                .cliente(cliente)
                .nome(request.getNome())
                .descricao(blankToNull(request.getDescricao()))
                .status(request.getStatus())
                .packageAndroid(blankToNull(request.getPackageAndroid()))
                .bundleIdIos(blankToNull(request.getBundleIdIos()))
                .versaoAndroid(blankToNull(request.getVersaoAndroid()))
                .versaoIos(blankToNull(request.getVersaoIos()))
                .urlAndroid(blankToNull(request.getUrlAndroid()))
                .urlIos(blankToNull(request.getUrlIos()))
                .iconeUrl(blankToNull(request.getIconeUrl()))
                .build();

        return MapperUtil.toResponse(aplicativoMobileRepository.save(aplicativo));
    }

    @Transactional
    public AplicativoMobileResponse atualizar(Long id, AplicativoMobileRequest request) {
        SecurityUtil.exigirAdmin();
        AplicativoMobile aplicativo = buscarEntidade(id);

        Long clienteId = autorizacaoService.resolverClienteId(request.getClienteId());
        Cliente cliente = buscarCliente(clienteId);

        aplicativo.setCliente(cliente);
        MapperUtil.updateEntity(aplicativo, request);
        return MapperUtil.toResponse(aplicativoMobileRepository.save(aplicativo));
    }

    @Transactional
    public void excluir(Long id) {
        SecurityUtil.exigirAdmin();
        AplicativoMobile aplicativo = buscarEntidade(id);
        arquivoService.excluir(aplicativo.getDocumentoRequisitosUrl());
        aplicativoMobileRepository.delete(aplicativo);
    }

    @Transactional
    public void excluirPorCliente(Long clienteId) {
        aplicativoMobileRepository.findByClienteId(clienteId).forEach(aplicativo -> {
            arquivoService.excluir(aplicativo.getDocumentoRequisitosUrl());
            aplicativoMobileRepository.delete(aplicativo);
        });
    }

    @Transactional
    public AplicativoMobileResponse enviarDocumento(Long id, MultipartFile documento) {
        SecurityUtil.exigirAdmin();
        if (documento == null || documento.isEmpty()) {
            throw new BusinessException("Envie um arquivo PDF");
        }

        AplicativoMobile aplicativo = buscarEntidade(id);

        arquivoService.excluir(aplicativo.getDocumentoRequisitosUrl());
        aplicativo.setDocumentoRequisitosUrl(arquivoService.salvarPdf(documento, PASTA_DOCUMENTOS));
        return MapperUtil.toResponse(aplicativoMobileRepository.save(aplicativo));
    }

    @Transactional
    public AplicativoMobileResponse removerDocumento(Long id) {
        SecurityUtil.exigirAdmin();
        AplicativoMobile aplicativo = buscarEntidade(id);

        arquivoService.excluir(aplicativo.getDocumentoRequisitosUrl());
        aplicativo.setDocumentoRequisitosUrl(null);
        return MapperUtil.toResponse(aplicativoMobileRepository.save(aplicativo));
    }

    private AplicativoMobile buscarEntidade(Long id) {
        AplicativoMobile aplicativo = aplicativoMobileRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Aplicativo mobile não encontrado com id: " + id));
        autorizacaoService.validarAcessoAplicativoMobile(id);
        return aplicativo;
    }

    private Cliente buscarCliente(Long clienteId) {
        return clienteRepository.findById(clienteId)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente não encontrado com id: " + clienteId));
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
