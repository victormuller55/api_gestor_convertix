package br.net.convertix.gestor.service;

import br.net.convertix.gestor.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.UUID;

@Service
@Slf4j
public class ArquivoService {

    private static final Set<String> TIPOS_PERMITIDOS = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp"
    );

    private static final String URL_PREFIX = "/uploads/";

    private final Path diretorioBase;

    public ArquivoService(@Value("${app.upload.dir:uploads}") String uploadDir) {
        this.diretorioBase = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(diretorioBase);
        } catch (IOException e) {
            throw new IllegalStateException("Não foi possível criar o diretório de uploads", e);
        }
    }

    public String salvar(MultipartFile arquivo, String subpasta) {
        if (arquivo == null || arquivo.isEmpty()) {
            return null;
        }

        String contentType = arquivo.getContentType();
        if (contentType == null || !TIPOS_PERMITIDOS.contains(contentType)) {
            throw new BusinessException("Formato de imagem não suportado. Use JPEG, PNG ou WebP");
        }

        String extensao = extensaoPorContentType(contentType);
        String nomeArquivo = UUID.randomUUID() + extensao;

        try {
            Path destino = diretorioBase.resolve(subpasta);
            Files.createDirectories(destino);
            Path caminhoArquivo = destino.resolve(nomeArquivo);
            arquivo.transferTo(caminhoArquivo);
            return URL_PREFIX + subpasta + "/" + nomeArquivo;
        } catch (IOException e) {
            log.error("Erro ao salvar arquivo em {}", subpasta, e);
            throw new BusinessException("Erro ao salvar a imagem");
        }
    }

    public void excluir(String urlPath) {
        if (urlPath == null || urlPath.isBlank() || !urlPath.startsWith(URL_PREFIX)) {
            return;
        }

        String caminhoRelativo = urlPath.substring(URL_PREFIX.length());
        Path caminhoArquivo = diretorioBase.resolve(caminhoRelativo).normalize();

        if (!caminhoArquivo.startsWith(diretorioBase)) {
            return;
        }

        try {
            Files.deleteIfExists(caminhoArquivo);
        } catch (IOException e) {
            log.warn("Não foi possível excluir o arquivo: {}", urlPath, e);
        }
    }

    private String extensaoPorContentType(String contentType) {
        return switch (contentType) {
            case "image/jpeg" -> ".jpg";
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            default -> ".jpg";
        };
    }
}
