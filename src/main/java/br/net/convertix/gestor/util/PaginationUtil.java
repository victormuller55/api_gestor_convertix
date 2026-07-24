package br.net.convertix.gestor.util;

import br.net.convertix.gestor.dto.response.PageResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.util.function.Function;

public final class PaginationUtil {

    public static final int DEFAULT_SIZE = 30;
    public static final int MAX_SIZE = 100;

    private PaginationUtil() {
    }

    public static PageRequest of(int page, int size) {
        return PageRequest.of(normalizarPage(page), normalizarSize(size));
    }

    public static PageRequest of(int page, int size, Sort sort) {
        return PageRequest.of(normalizarPage(page), normalizarSize(size), sort);
    }

    public static <T, R> PageResponse<R> toResponse(Page<T> page, Function<T, R> mapper) {
        return PageResponse.<R>builder()
                .content(page.getContent().stream().map(mapper).toList())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .build();
    }

    public static <R> PageResponse<R> toResponse(Page<?> page, java.util.List<R> content) {
        return PageResponse.<R>builder()
                .content(content)
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .build();
    }

    private static int normalizarPage(int page) {
        return Math.max(page, 0);
    }

    private static int normalizarSize(int size) {
        return Math.min(Math.max(size, 1), MAX_SIZE);
    }
}
