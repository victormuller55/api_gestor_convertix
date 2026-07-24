package br.net.convertix.gestor.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

@Service
public class JwtService {

    private final SecretKey secretKey;
    private final String issuer;
    private final String audience;
    private final Duration expiration;
    private final Duration clockSkew;

    public JwtService(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.issuer}") String issuer,
            @Value("${jwt.audience}") String audience,
            @Value("${jwt.expiration-hours:24}") long expirationHours,
            @Value("${jwt.clock-skew-seconds:30}") long clockSkewSeconds) {
        if (!StringUtils.hasText(secret) || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("jwt.secret deve ter no mínimo 32 bytes");
        }
        this.secretKey = Keys.hmacShaKeyFor(gerarChave(secret));
        this.issuer = issuer;
        this.audience = audience;
        this.expiration = Duration.ofHours(expirationHours);
        this.clockSkew = Duration.ofSeconds(clockSkewSeconds);
    }

    public String gerarToken(UsuarioAutenticado usuario) {
        Instant agora = Instant.now();
        Instant expiracao = agora.plus(expiration);

        return Jwts.builder()
                .issuer(issuer)
                .audience().add(audience).and()
                .subject(usuario.getId().toString())
                .claim("email", usuario.getEmail())
                .claim("tipo", usuario.getTipo().name())
                .claim("cliente_id", usuario.getClienteId())
                .issuedAt(Date.from(agora))
                .expiration(Date.from(expiracao))
                .signWith(secretKey)
                .compact();
    }

    public UsuarioAutenticado extrairUsuario(String token) {
        var claims = Jwts.parser()
                .verifyWith(secretKey)
                .requireIssuer(issuer)
                .requireAudience(audience)
                .clockSkewSeconds(clockSkew.toSeconds())
                .build()
                .parseSignedClaims(token)
                .getPayload();

        Long clienteId = claims.get("cliente_id", Number.class) != null
                ? claims.get("cliente_id", Number.class).longValue()
                : null;

        return UsuarioAutenticado.builder()
                .id(Long.parseLong(claims.getSubject()))
                .email(claims.get("email", String.class))
                .tipo(br.net.convertix.gestor.enums.TipoUsuario.valueOf(claims.get("tipo", String.class)))
                .clienteId(clienteId)
                .build();
    }

    private byte[] gerarChave(String secret) {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length >= 32) {
            return keyBytes;
        }
        try {
            return MessageDigest.getInstance("SHA-256").digest(keyBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Erro ao gerar chave JWT", e);
        }
    }
}
