package com.example.demo.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PedidoDetailsAuthorizationIntegrationTest {

    private static final String DETAILS_ROUTE = "/backoffice/pedidos/OS-NAO-EXISTE/detalhes";

    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminPodeAcessarRotaDeDetalhes() throws Exception {
        mockMvc.perform(get(DETAILS_ROUTE)).andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "SUPPORT_MANAGER")
    void suporteGestorPodeAcessarRotaDeDetalhes() throws Exception {
        mockMvc.perform(get(DETAILS_ROUTE)).andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "USER")
    void usuarioSolicitanteNaoPodeAcessarRotaDeDetalhes() throws Exception {
        mockMvc.perform(get(DETAILS_ROUTE)).andExpect(status().isForbidden());
    }

    @Test
    void visitanteSemSessaoNaoPodeAcessarRotaDeDetalhes() throws Exception {
        mockMvc.perform(get(DETAILS_ROUTE)).andExpect(status().isForbidden());
    }
}
