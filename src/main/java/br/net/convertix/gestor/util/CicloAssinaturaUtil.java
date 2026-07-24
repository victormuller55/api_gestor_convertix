package br.net.convertix.gestor.util;

import br.net.convertix.gestor.enums.CicloAssinatura;

import java.time.LocalDate;

public final class CicloAssinaturaUtil {

    private CicloAssinaturaUtil() {
    }

    public static LocalDate calcularProxima(LocalDate dataBase, CicloAssinatura ciclo) {
        if (dataBase == null || ciclo == null) {
            return null;
        }
        return switch (ciclo) {
            case WEEKLY -> dataBase.plusWeeks(1);
            case BIWEEKLY -> dataBase.plusWeeks(2);
            case MONTHLY -> dataBase.plusMonths(1);
            case BIMONTHLY -> dataBase.plusMonths(2);
            case QUARTERLY -> dataBase.plusMonths(3);
            case SEMIANNUALLY -> dataBase.plusMonths(6);
            case YEARLY -> dataBase.plusYears(1);
        };
    }
}
