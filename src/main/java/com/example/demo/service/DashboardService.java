package com.example.demo.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.example.demo.model.IndicadorDesempenhoDTO;
import com.example.demo.model.Pedido;
import com.example.demo.model.PontoGraficoDTO;
import com.example.demo.repository.PedidoRepository;

@Service
public class DashboardService {

    private static final int STATUS_CONCLUIDA = 5;
    private static final Locale PT_BR = Locale.of("pt", "BR");

    private final PedidoRepository pedidoRepository;

    public DashboardService(PedidoRepository pedidoRepository) {
        this.pedidoRepository = pedidoRepository;
    }

    // Retorna SOMENTE os dados do MTBF (de JAN até o mês escolhido do ano escolhido)
    public IndicadorDesempenhoDTO obterMtbf(Integer ano, Integer mes) {
        YearMonth limite = resolverPeriodo(ano, mes);
        List<Pedido> pedidosDoPeriodo = buscarConcluidasNoAnoAte(limite);

        List<PontoGraficoDTO> pontos = new ArrayList<>();
        for (int m = 1; m <= limite.getMonthValue(); m++) {
            int mesAtual = m;
            // Acumulado de JAN até o mês 'm' para conseguir comparar falhas do mesmo PC no ano
            List<Pedido> ateOMes = pedidosDoPeriodo.stream()
                    .filter(p -> p.getDataHoraConclusao().getMonthValue() <= mesAtual)
                    .toList();

            pontos.add(new PontoGraficoDTO(formatarMesCurto(limite.getYear(), m), calcularMtbfDias(ateOMes)));
        }

        return new IndicadorDesempenhoDTO(
                formatarPeriodo(limite),
                calcularMtbfDias(pedidosDoPeriodo),
                pontos
        );
    }

    // Retorna SOMENTE os dados do MTTR (de JAN até o mês escolhido do ano escolhido)
    public IndicadorDesempenhoDTO obterMttr(Integer ano, Integer mes) {
        YearMonth limite = resolverPeriodo(ano, mes);
        List<Pedido> pedidosDoPeriodo = buscarConcluidasNoAnoAte(limite);

        List<PontoGraficoDTO> pontos = new ArrayList<>();
        for (int m = 1; m <= limite.getMonthValue(); m++) {
            int mesAtual = m;

            Long mttrMinutos = calcularMttrMinutos(pedidosDoPeriodo.stream()
                    .filter(p -> p.getDataHoraConclusao().getMonthValue() == mesAtual)
                    .toList());
            Double mttrHoras = mttrMinutos != null ? arredondar(mttrMinutos / 60.0) : 0.0;

            pontos.add(new PontoGraficoDTO(formatarMesCurto(limite.getYear(), m), mttrHoras));
        }

        Long mttrGeralMinutos = calcularMttrMinutos(pedidosDoPeriodo);
        return new IndicadorDesempenhoDTO(
                formatarPeriodo(limite),
                mttrGeralMinutos != null ? mttrGeralMinutos.doubleValue() : null,
                pontos
        );
    }

    private List<Pedido> buscarConcluidasNoAnoAte(YearMonth limite) {
        LocalDateTime inicioAno = LocalDateTime.of(limite.getYear(), 1, 1, 0, 0);
        LocalDateTime fimMes = limite.atEndOfMonth().atTime(23, 59, 59);
        return pedidoRepository.findByStatusAndDataHoraConclusaoBetween(STATUS_CONCLUIDA, inicioAno, fimMes);
    }

    private Double calcularMtbfDias(List<Pedido> pedidos) {
        Map<Object, List<Pedido>> porPc = pedidos.stream()
                .filter(p -> p.getIdPC() != null && p.getDataHoraAbertura() != null)
                .collect(Collectors.groupingBy(Pedido::getIdPC));

        List<Double> intervalosDias = new ArrayList<>();
        for (List<Pedido> lista : porPc.values()) {
            if (lista.size() < 2) continue;
            lista.sort(Comparator.comparing(Pedido::getDataHoraAbertura));

            for (int i = 1; i < lista.size(); i++) {
                long mins = Duration.between(
                        lista.get(i - 1).getDataHoraAbertura(),
                        lista.get(i).getDataHoraAbertura()
                ).toMinutes();
                intervalosDias.add(mins / 1440.0);
            }
        }

        if (intervalosDias.isEmpty()) return null;
        double media = intervalosDias.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        return arredondar(media);
    }

    private Long calcularMttrMinutos(List<Pedido> pedidos) {
        List<Long> tempos = pedidos.stream()
                .filter(p -> p.getDataHoraInicioDiagnostico() != null && p.getDataHoraConclusao() != null)
                .map(p -> {
                    long total = Duration.between(p.getDataHoraInicioDiagnostico(), p.getDataHoraConclusao()).toMinutes();
                    if (p.getDataHoraProntaRetirada() != null) {
                        long espera = Duration.between(p.getDataHoraProntaRetirada(), p.getDataHoraConclusao()).toMinutes();
                        total -= Math.max(0, espera);
                    }
                    return Math.max(0, total);
                })
                .toList();

        if (tempos.isEmpty()) return null;
        return Math.round(tempos.stream().mapToLong(Long::longValue).average().orElse(0));
    }

    private YearMonth resolverPeriodo(Integer ano, Integer mes) {
        YearMonth agora = YearMonth.now();
        return YearMonth.of(ano != null ? ano : agora.getYear(), mes != null ? mes : agora.getMonthValue());
    }

    private String formatarPeriodo(YearMonth ym) {
        return ym.format(DateTimeFormatter.ofPattern("MMM/yyyy", PT_BR)).toUpperCase().replace(".", "");
    }

    private String formatarMesCurto(int ano, int mes) {
        return YearMonth.of(ano, mes).format(DateTimeFormatter.ofPattern("MMM", PT_BR)).toUpperCase().replace(".", "");
    }

    private double arredondar(double valor) {
        return Math.round(valor * 10.0) / 10.0;
    }
}