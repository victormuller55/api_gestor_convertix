package br.net.convertix.gestor.config;

import br.net.convertix.gestor.entity.Usuario;
import br.net.convertix.gestor.enums.TipoUsuario;
import br.net.convertix.gestor.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Profile("!test")
@RequiredArgsConstructor
public class AdminInitializer implements ApplicationRunner {

    private static final String EMAIL_PADRAO = "victor@convertix.net.br";
    private static final String SENHA_PADRAO = "123aaa";

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(ApplicationArguments args) {
        if (usuarioRepository.findByEmail(EMAIL_PADRAO).isPresent()) {
            return;
        }

        Usuario admin = Usuario.builder()
                .nome("Administrador")
                .email(EMAIL_PADRAO)
                .senha(passwordEncoder.encode(SENHA_PADRAO))
                .ativo(true)
                .tipo(TipoUsuario.ADMIN)
                .build();

        usuarioRepository.save(admin);
        log.info("Usuário admin padrão criado: {}", EMAIL_PADRAO);
    }
}
