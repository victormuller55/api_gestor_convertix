package br.net.convertix.gestor.config;

import br.net.convertix.gestor.entity.Usuario;
import br.net.convertix.gestor.enums.TipoUsuario;
import br.net.convertix.gestor.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Slf4j
@Component
@Profile("local")
@RequiredArgsConstructor
public class AdminInitializer implements ApplicationRunner {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.auto-create:false}")
    private boolean autoCreate;

    @Value("${app.admin.email:}")
    private String email;

    @Value("${app.admin.password:}")
    private String senha;

    @Override
    public void run(ApplicationArguments args) {
        if (!autoCreate || !StringUtils.hasText(email) || !StringUtils.hasText(senha)) {
            return;
        }

        if (senha.length() < 8) {
            log.warn("Admin padrão não criado: senha configurada é muito curta");
            return;
        }

        if (usuarioRepository.findByEmail(email).isPresent()) {
            return;
        }

        Usuario admin = Usuario.builder()
                .nome("Administrador")
                .email(email)
                .senha(passwordEncoder.encode(senha))
                .ativo(true)
                .tipo(TipoUsuario.ADMIN)
                .build();

        usuarioRepository.save(admin);
        log.info("Usuário admin padrão criado para ambiente local");
    }
}
