package br.net.convertix.gestor.service;

import br.net.convertix.gestor.dto.request.LoginRequest;
import br.net.convertix.gestor.dto.response.LoginResponse;
import br.net.convertix.gestor.entity.Cliente;
import br.net.convertix.gestor.entity.Usuario;
import br.net.convertix.gestor.enums.TipoUsuario;
import br.net.convertix.gestor.exception.BusinessException;
import br.net.convertix.gestor.exception.UnauthorizedException;
import br.net.convertix.gestor.repository.ClienteRepository;
import br.net.convertix.gestor.repository.UsuarioRepository;
import br.net.convertix.gestor.security.JwtService;
import br.net.convertix.gestor.security.UsuarioAutenticado;
import br.net.convertix.gestor.util.MapperUtil;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final String CREDENCIAIS_INVALIDAS = "Usuário ou senha inválidos.";

    private final UsuarioRepository usuarioRepository;
    private final ClienteRepository clienteRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    private String dummyPasswordHash;

    @PostConstruct
    void initTimingSafeHash() {
        this.dummyPasswordHash = passwordEncoder.encode("timing-safe-dummy");
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        Usuario usuario = usuarioRepository.findByEmail(request.getEmail()).orElse(null);

        // Mitiga timing attack: sempre executa matches, mesmo se o e-mail não existir
        String hash = usuario != null ? usuario.getSenha() : dummyPasswordHash;
        boolean senhaOk = passwordEncoder.matches(request.getSenha(), hash);

        if (usuario == null || !Boolean.TRUE.equals(usuario.getAtivo()) || !senhaOk) {
            throw new UnauthorizedException(CREDENCIAIS_INVALIDAS);
        }

        Cliente cliente = null;
        Long clienteId = null;

        if (usuario.getTipo() == TipoUsuario.CLIENTE) {
            cliente = clienteRepository.findByUsuarioId(usuario.getId())
                    .orElseThrow(() -> new BusinessException("Usuário cliente sem empresa vinculada"));
            clienteId = cliente.getId();
        }

        UsuarioAutenticado usuarioAutenticado = UsuarioAutenticado.builder()
                .id(usuario.getId())
                .email(usuario.getEmail())
                .tipo(usuario.getTipo())
                .clienteId(clienteId)
                .build();

        String token = jwtService.gerarToken(usuarioAutenticado);
        return MapperUtil.toLoginResponse(usuario, cliente, token);
    }
}
