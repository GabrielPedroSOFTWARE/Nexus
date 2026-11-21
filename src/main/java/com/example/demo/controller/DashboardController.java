package com.example.demo.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.model.IndicadorDesempenhoDTO;
import com.example.demo.service.DashboardService;

@RestController
@RequestMapping("/backoffice/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/mtbf")
    public ResponseEntity<IndicadorDesempenhoDTO> obterMtbf(
            @RequestParam(name = "ano", required = false) Integer ano,
            @RequestParam(name = "mes", required = false) Integer mes) {
        return ResponseEntity.ok(dashboardService.obterMtbf(ano, mes));
    }

    @GetMapping("/mttr")
    public ResponseEntity<IndicadorDesempenhoDTO> obterMttr(
            @RequestParam(name = "ano", required = false) Integer ano,
            @RequestParam(name = "mes", required = false) Integer mes) {
        return ResponseEntity.ok(dashboardService.obterMttr(ano, mes));
    }
}