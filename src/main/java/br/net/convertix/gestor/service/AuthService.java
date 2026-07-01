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
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final ClienteRepository clienteRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        Usuario usuario = usuarioRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UnauthorizedException("Email ou senha inválidos"));

        if (!Boolean.TRUE.equals(usuario.getAtivo())) {
            throw new UnauthorizedException("Usuário inativo");
        }

        if (!passwordEncoder.matches(request.getSenha(), usuario.getSenha())) {
            throw new UnauthorizedException("Email ou senha inválidos");
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
