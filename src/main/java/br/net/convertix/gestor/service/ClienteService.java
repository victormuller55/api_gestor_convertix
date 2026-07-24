package br.net.convertix.gestor.service;

import br.net.convertix.gestor.dto.request.ClienteRequest;
import br.net.convertix.gestor.dto.response.ClienteResponse;
import br.net.convertix.gestor.dto.response.PageResponse;
import br.net.convertix.gestor.entity.Cliente;
import br.net.convertix.gestor.entity.Usuario;
import br.net.convertix.gestor.exception.BusinessException;
import br.net.convertix.gestor.exception.ResourceNotFoundException;
import br.net.convertix.gestor.repository.ClienteRepository;
import br.net.convertix.gestor.repository.SiteRepository;
import br.net.convertix.gestor.repository.UsuarioRepository;
import br.net.convertix.gestor.repository.spec.ClienteSpecification;
import br.net.convertix.gestor.util.DocumentoUtil;
import br.net.convertix.gestor.util.MapperUtil;
import br.net.convertix.gestor.util.PaginationUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ClienteService {

    private static final String PASTA_FOTOS = "usuarios";

    private final ClienteRepository clienteRepository;
    private final UsuarioRepository usuarioRepository;
    private final SiteRepository siteRepository;
    private final PasswordEncoder passwordEncoder;
    private final ArquivoService arquivoService;
    private final SiteService siteService;

    @Transactional(readOnly = true)
    public PageResponse<ClienteResponse> buscar(Long id, String query, int page, int size) {
        Page<Cliente> resultado = clienteRepository.findAll(
                ClienteSpecification.comFiltros(id, query),
                PaginationUtil.of(page, size));
        return PaginationUtil.toResponse(resultado, MapperUtil::toResponse);
    }

    @Transactional(readOnly = true)
    public List<ClienteResponse> listarTodos() {
        return clienteRepository.findAll().stream()
                .map(MapperUtil::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ClienteResponse buscarPorId(Long id) {
        return clienteRepository.findById(id)
                .map(MapperUtil::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente não encontrado com id: " + id));
    }

    @Transactional
    public ClienteResponse criar(ClienteRequest request, MultipartFile foto) {
        String documento = validarDocumento(request.getDocumento());

        if (usuarioRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException("Já existe um usuário com o email: " + request.getEmail());
        }

        if (clienteRepository.existsByDocumento(documento)) {
            throw new BusinessException("Já existe um cliente com o documento: " + documento);
        }

        request.setDocumento(documento);

        Usuario usuario = MapperUtil.toUsuarioFromCliente(request);
        usuario.setSenha(passwordEncoder.encode(request.getSenha()));
        usuario.setFoto(arquivoService.salvar(foto, PASTA_FOTOS));
        usuario = usuarioRepository.save(usuario);
        Cliente cliente = MapperUtil.toEntity(request, usuario);

        return MapperUtil.toResponse(clienteRepository.save(cliente));
    }

    @Transactional
    public ClienteResponse atualizar(Long id, ClienteRequest request, MultipartFile foto) {
        Cliente cliente = clienteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente não encontrado com id: " + id));

        Usuario usuario = cliente.getUsuario();
        if (usuario == null) {
            throw new BusinessException("Cliente não possui usuário vinculado");
        }

        String documento = validarDocumento(request.getDocumento());
        request.setDocumento(documento);

        if (usuarioRepository.existsByEmailAndIdNot(request.getEmail(), usuario.getId())) {
            throw new BusinessException("Já existe um usuário com o email: " + request.getEmail());
        }

        if (clienteRepository.existsByDocumentoAndIdNot(documento, id)) {
            throw new BusinessException("Já existe um cliente com o documento: " + documento);
        }

        MapperUtil.updateEntity(cliente, request);
        MapperUtil.updateUsuarioFromCliente(usuario, request);
        if (request.getSenha() != null && !request.getSenha().isBlank()) {
            usuario.setSenha(passwordEncoder.encode(request.getSenha()));
        }
        atualizarFoto(usuario, foto);

        usuarioRepository.save(usuario);
        return MapperUtil.toResponse(clienteRepository.save(cliente));
    }

    @Transactional
    public void excluir(Long id) {
        Cliente cliente = clienteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente não encontrado com id: " + id));

        Usuario usuario = cliente.getUsuario();

        siteRepository.findByClienteId(id).stream()
                .map(site -> site.getId())
                .toList()
                .forEach(siteService::excluir);

        if (usuario != null) {
            usuario.setCliente(null);
        }
        cliente.setUsuario(null);
        clienteRepository.delete(cliente);

        if (usuario != null) {
            arquivoService.excluir(usuario.getFoto());
            usuarioRepository.delete(usuario);
        }
    }

    private void atualizarFoto(Usuario usuario, MultipartFile foto) {
        if (foto == null || foto.isEmpty()) {
            return;
        }
        arquivoService.excluir(usuario.getFoto());
        usuario.setFoto(arquivoService.salvar(foto, PASTA_FOTOS));
    }

    private String validarDocumento(String documento) {
        String documentoNormalizado = DocumentoUtil.normalizar(documento);

        if (!DocumentoUtil.validar(documentoNormalizado)) {
            throw new BusinessException("Documento inválido");
        }

        return documentoNormalizado;
    }
}
