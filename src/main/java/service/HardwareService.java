package service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import oshi.SystemInfo;
import oshi.hardware.GlobalMemory;
import oshi.hardware.GraphicsCard;
import oshi.hardware.HWDiskStore;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.hardware.PhysicalMemory;

@Service
public class HardwareService {

	// Gera o relatório com as peças do pc
    public Map<String, Object> gerarRelatorioCompleto() {
        Map<String, Object> relatorio = new LinkedHashMap<>();
        SystemInfo informacoes = new SystemInfo();
        HardwareAbstractionLayer hardware = informacoes.getHardware();

        relatorio.put("Fabricante", hardware.getComputerSystem().getManufacturer());
        relatorio.put("Modelo", hardware.getComputerSystem().getModel());
        relatorio.put("S.O.", informacoes.getOperatingSystem().getFamily() + " " + informacoes.getOperatingSystem().getVersionInfo().getVersion());
        relatorio.put("CPU", hardware.getProcessor().getProcessorIdentifier().getName());

        relatorio.putAll(lerMemoria(hardware.getMemory()));
        relatorio.put("GPU", lerPlacasDeVideo(hardware.getGraphicsCards()));
        relatorio.put("Armazenamento", lerDiscos(hardware.getDiskStores()));

        return relatorio;
    }

    // Lê memória a RAM
    private Map<String, String> lerMemoria(GlobalMemory memoria) {
        long ramTotal = memoria.getTotal() / (1024 * 1024 * 1024);
        List<String> detalhes = new ArrayList<>();
        
        for (PhysicalMemory pente : memoria.getPhysicalMemory()) {
            long capacidade = pente.getCapacity() / (1024 * 1024 * 1024);
            long velocidade = pente.getClockSpeed() > 0 ? pente.getClockSpeed() / 1_000_000 : 0;
            String tipo = pente.getMemoryType();
            detalhes.add(capacidade + "GB" + (!tipo.isEmpty() ? ", " + tipo : "") + (velocidade > 0 ? ", " + velocidade + " MHz" : ""));
        }
        
        return Map.of(
            "RAM", ramTotal + " GB",
            "Mais Detalhes - Memória", detalhes.isEmpty() ? ramTotal + " GB" : String.join(" | ", detalhes)
        );
    }

    // Verifica se existe alguma placa de vídeo cadastrada
    private Object lerPlacasDeVideo(List<GraphicsCard> gpus) {
        List<String> placas = new ArrayList<>();
        for (GraphicsCard gpu : gpus) {
            placas.add(gpu.getName());
        }
        return placas.isEmpty() ? "Não detectada" : placas;
    }

    // Verifica se há algum disco rígido
    private List<String> lerDiscos(List<HWDiskStore> discos) {
        List<String> lista = new ArrayList<>();
        for (HWDiskStore disco : discos) {
            long tamanho = disco.getSize() / (1024 * 1024 * 1024);
            if (tamanho > 0) lista.add(disco.getModel() + " (" + tamanho + " GB)");
        }
        return lista;
    }
}