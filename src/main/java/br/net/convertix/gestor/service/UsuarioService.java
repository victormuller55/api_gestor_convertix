package br.net.convertix.gestor.service;

import br.net.convertix.gestor.dto.request.UsuarioRequest;
import br.net.convertix.gestor.dto.response.UsuarioResponse;
import br.net.convertix.gestor.entity.Usuario;
import br.net.convertix.gestor.enums.TipoUsuario;
import br.net.convertix.gestor.exception.BusinessException;
import br.net.convertix.gestor.exception.ResourceNotFoundException;
import br.net.convertix.gestor.repository.ClienteRepository;
import br.net.convertix.gestor.repository.UsuarioRepository;
import br.net.convertix.gestor.repository.spec.UsuarioSpecification;
import br.net.convertix.gestor.security.SecurityUtil;
import br.net.convertix.gestor.util.MapperUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UsuarioService {

    private static final String PASTA_FOTOS = "usuarios";

    private final UsuarioRepository usuarioRepository;
    private final ClienteRepository clienteRepository;
    private final PasswordEncoder passwordEncoder;
    private final ArquivoService arquivoService;

    @Transactional(readOnly = true)
    public List<UsuarioResponse> buscar(Long id, String query, Boolean ativo) {
        return usuarioRepository.findAll(UsuarioSpecification.comFiltros(id, query, ativo)).stream()
                .map(MapperUtil::toResponse)
                .toList();
    }

    @Transactional
    public UsuarioResponse criar(UsuarioRequest request, MultipartFile foto) {
        SecurityUtil.exigirAdmin();

        if (usuarioRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException("Já existe um usuário com o email: " + request.getEmail());
        }

        Usuario usuario = MapperUtil.toAdminEntity(request);
        usuario.setSenha(passwordEncoder.encode(request.getSenha()));
        usuario.setFoto(arquivoService.salvar(foto, PASTA_FOTOS));
        return MapperUtil.toResponse(usuarioRepository.save(usuario));
    }

    @Transactional
    public UsuarioResponse atualizar(Long id, UsuarioRequest request, MultipartFile foto) {
        SecurityUtil.exigirAdmin();

        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado com id: " + id));

        if (usuario.getTipo() == TipoUsuario.CLIENTE) {
            throw new BusinessException("Usuários cliente devem ser alterados pelo cadastro de cliente");
        }

        if (usuarioRepository.existsByEmailAndIdNot(request.getEmail(), id)) {
            throw new BusinessException("Já existe um usuário com o email: " + request.getEmail());
        }

        MapperUtil.updateEntity(usuario, request);
        if (request.getSenha() != null && !request.getSenha().isBlank()) {
            usuario.setSenha(passwordEncoder.encode(request.getSenha()));
        }
        atualizarFoto(usuario, foto);
        return MapperUtil.toResponse(usuarioRepository.save(usuario));
    }

    @Transactional
    public void excluir(Long id) {
        SecurityUtil.exigirAdmin();

        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado com id: " + id));

        if (usuario.getTipo() == TipoUsuario.CLIENTE) {
            throw new BusinessException("Usuários cliente devem ser excluídos pelo cadastro de cliente");
        }

        if (clienteRepository.existsByUsuarioId(id)) {
            throw new BusinessException("Não é possível excluir um usuário vinculado a um cliente");
        }

        arquivoService.excluir(usuario.getFoto());
        usuarioRepository.deleteById(id);
    }

    private void atualizarFoto(Usuario usuario, MultipartFile foto) {
        if (foto == null || foto.isEmpty()) {
            return;
        }
        arquivoService.excluir(usuario.getFoto());
        usuario.setFoto(arquivoService.salvar(foto, PASTA_FOTOS));
    }
}
