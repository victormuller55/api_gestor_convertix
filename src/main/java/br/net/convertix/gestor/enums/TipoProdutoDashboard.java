package br.net.convertix.gestor.enums;

import java.util.Locale;

public enum TipoProdutoDashboard {
    TODOS,
    BIOLINK,
    LANDING_PAGE,
    SITE_COMERCIAL,
    APLICATIVO_MOBILE;

    public static TipoProdutoDashboard fromParam(String raw) {
        if (raw == null || raw.isBlank()) {
            return TODOS;
        }
        try {
            return valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return TODOS;
        }
    }

    public boolean inclui(String produtoTipo) {
        if (this == TODOS) {
            return true;
        }
        return name().equals(produtoTipo);
    }

    public String label() {
        return switch (this) {
            case TODOS -> "Todos os produtos";
            case BIOLINK -> "BioLink";
            case LANDING_PAGE -> "Landing page";
            case SITE_COMERCIAL -> "Site institucional";
            case APLICATIVO_MOBILE -> "Aplicativo mobile";
        };
    }
}
