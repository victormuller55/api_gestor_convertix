package br.net.convertix.gestor.service;

import br.net.convertix.gestor.dto.request.LoginRequest;
import br.net.convertix.gestor.dto.request.RecuperarSenhaRequest;
import br.net.convertix.gestor.dto.request.RedefinirSenhaRequest;
import br.net.convertix.gestor.dto.request.VerificarCodigoRecuperacaoRequest;
import br.net.convertix.gestor.dto.response.LoginResponse;
import br.net.convertix.gestor.dto.response.RecuperarSenhaResponse;
import br.net.convertix.gestor.dto.response.RedefinirSenhaResponse;
import br.net.convertix.gestor.dto.response.VerificarCodigoRecuperacaoResponse;
import br.net.convertix.gestor.entity.Cliente;
import br.net.convertix.gestor.entity.RecuperacaoSenha;
import br.net.convertix.gestor.entity.Usuario;
import br.net.convertix.gestor.enums.TipoUsuario;
import br.net.convertix.gestor.exception.BusinessException;
import br.net.convertix.gestor.exception.UnauthorizedException;
import br.net.convertix.gestor.repository.ClienteRepository;
import br.net.convertix.gestor.repository.RecuperacaoSenhaRepository;
import br.net.convertix.gestor.repository.UsuarioRepository;
import br.net.convertix.gestor.security.JwtService;
import br.net.convertix.gestor.security.UsuarioAutenticado;
import br.net.convertix.gestor.util.MapperUtil;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private static final String CREDENCIAIS_INVALIDAS = "Usuário ou senha inválidos.";
    private static final String RECUPERACAO_INDISPONIVEL =
            "Não foi possível processar a solicitação de recuperação de senha.";
    private static final String CODIGO_INVALIDO = "Código inválido ou expirado.";
    private static final int CODIGO_EXPIRACAO_HORAS = 2;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final UsuarioRepository usuarioRepository;
    private final ClienteRepository clienteRepository;
    private final RecuperacaoSenhaRepository recuperacaoSenhaRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final EmailService emailService;

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

    @Transactional
    public RecuperarSenhaResponse recuperarSenha(RecuperarSenhaRequest request) {
        Usuario usuario = usuarioRepository.findByEmail(request.getEmail()).orElse(null);

        if (usuario == null || !Boolean.TRUE.equals(usuario.getAtivo())) {
            throw new BusinessException(RECUPERACAO_INDISPONIVEL);
        }

        String codigo = gerarCodigoRecuperacao();
        LocalDateTime enviadoEm = LocalDateTime.now();

        RecuperacaoSenha recuperacao = RecuperacaoSenha.builder()
                .usuario(usuario)
                .codigo(passwordEncoder.encode(codigo))
                .enviadoEm(enviadoEm)
                .build();
        recuperacaoSenhaRepository.save(recuperacao);

        emailService.enviarCodigoRecuperacaoSenha(usuario.getEmail(), usuario.getNome(), codigo);
        log.info("Código de recuperação de senha gerado para o usuário id={}", usuario.getId());

        return RecuperarSenhaResponse.builder()
                .usuarioId(usuario.getId())
                .mensagem("Código de recuperação enviado para o e-mail cadastrado.")
                .enviadoEm(enviadoEm)
                .expiraEm(enviadoEm.plusHours(CODIGO_EXPIRACAO_HORAS))
                .build();
    }

    @Transactional(readOnly = true)
    public VerificarCodigoRecuperacaoResponse verificarCodigo(VerificarCodigoRecuperacaoRequest request) {
        validarCodigoRecuperacao(request.getUsuarioId(), request.getCodigo());

        return VerificarCodigoRecuperacaoResponse.builder()
                .usuarioId(request.getUsuarioId())
                .valido(true)
                .mensagem("Código verificado com sucesso.")
                .build();
    }

    @Transactional
    public RedefinirSenhaResponse redefinirSenha(RedefinirSenhaRequest request) {
        Usuario usuario = validarCodigoRecuperacao(request.getUsuarioId(), request.getCodigo());

        usuario.setSenha(passwordEncoder.encode(request.getNovaSenha()));
        usuarioRepository.save(usuario);
        recuperacaoSenhaRepository.deleteByUsuarioId(usuario.getId());

        try {
            emailService.enviarAvisoSenhaAlterada(usuario.getEmail(), usuario.getNome());
        } catch (BusinessException ex) {
            log.warn("Senha redefinida para o usuário id={}, mas falha ao enviar e-mail de aviso",
                    usuario.getId());
        }

        log.info("Senha redefinida com sucesso para o usuário id={}", usuario.getId());

        return RedefinirSenhaResponse.builder()
                .usuarioId(usuario.getId())
                .mensagem("Senha alterada com sucesso.")
                .build();
    }

    private Usuario validarCodigoRecuperacao(Long usuarioId, String codigo) {
        Usuario usuario = usuarioRepository.findById(usuarioId).orElse(null);
        if (usuario == null || !Boolean.TRUE.equals(usuario.getAtivo())) {
            throw new BusinessException(CODIGO_INVALIDO);
        }

        RecuperacaoSenha ultimo = recuperacaoSenhaRepository
                .findFirstByUsuarioIdOrderByEnviadoEmDesc(usuarioId)
                .orElse(null);

        if (ultimo == null) {
            throw new BusinessException(CODIGO_INVALIDO);
        }

        LocalDateTime limite = ultimo.getEnviadoEm().plusHours(CODIGO_EXPIRACAO_HORAS);
        boolean expirado = LocalDateTime.now().isAfter(limite);
        boolean codigoOk = passwordEncoder.matches(codigo, ultimo.getCodigo());

        if (expirado || !codigoOk) {
            throw new BusinessException(CODIGO_INVALIDO);
        }

        return usuario;
    }

    private String gerarCodigoRecuperacao() {
        int numero = SECURE_RANDOM.nextInt(1_000_000);
        return String.format("%06d", numero);
    }
}
