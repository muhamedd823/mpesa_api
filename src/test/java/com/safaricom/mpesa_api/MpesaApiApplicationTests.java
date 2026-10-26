package com.safaricom.mpesa_api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.safaricom.mpesa_api.model.Transaction;
import com.safaricom.mpesa_api.service.TransactionCache;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class MpesaApiApplicationTests {

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper mapper;

    @Autowired
    TransactionCache transactionCache;

    private String registerAndLogin(String username, String password) throws Exception {
        String registerBody = mapper.writeValueAsString(Map.of("username", username, "password", password));
        mvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON).content(registerBody));

        String loginBody = mapper.writeValueAsString(Map.of("username", username, "password", password));
        MvcResult result = mvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON).content(loginBody))
                .andExpect(status().isOk())
                .andReturn();

        Map<?, ?> response = mapper.readValue(result.getResponse().getContentAsString(), Map.class);
        return (String) response.get("token");
    }

    @Test
    void registerSuccess() throws Exception {
        String body = mapper.writeValueAsString(Map.of("username", "testuser1", "password", "pass123"));
        mvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());
    }

    @Test
    void loginSuccess() throws Exception {
        registerAndLogin("loginuser", "pass123");
    }

    @Test
    void loginInvalidCredentials() throws Exception {
        String body = mapper.writeValueAsString(Map.of("username", "nobody", "password", "wrong"));
        mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void unauthorizedRequestReturns401() throws Exception {
        mvc.perform(get("/transactions/recent"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createTransactionSuccess() throws Exception {
        String token = registerAndLogin("txuser1", "pass123");
        String body = mapper.writeValueAsString(Map.of(
                "transactionType", "DEPOSIT",
                "amount", 1000.00,
                "currency", "ETB",
                "debitParty", "251700000001",
                "creditParty", "251700000002",
                "status", "SUCCESS",
                "timestamp", "2026-09-04T09:30:00"
        ));
        mvc.perform(post("/transactions")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.transactionType").value("DEPOSIT"));
    }

    @Test
    void createTransactionInvalidAmount() throws Exception {
        String token = registerAndLogin("txuser2", "pass123");
        String body = mapper.writeValueAsString(Map.of(
                "transactionType", "DEPOSIT",
                "amount", -50,
                "currency", "ETB",
                "debitParty", "251700000001",
                "creditParty", "251700000002",
                "status", "SUCCESS"
        ));
        mvc.perform(post("/transactions")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createTransactionInvalidType() throws Exception {
        String token = registerAndLogin("txuser3", "pass123");
        String body = mapper.writeValueAsString(Map.of(
                "transactionType", "INVALID_TYPE",
                "amount", 500,
                "currency", "ETB",
                "debitParty", "251700000001",
                "creditParty", "251700000002",
                "status", "SUCCESS"
        ));
        mvc.perform(post("/transactions")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void batchCreateTransactions() throws Exception {
        String token = registerAndLogin("batchuser", "pass123");
        List<Map<String, Object>> batch = List.of(
                Map.of("transactionType", "WITHDRAWAL", "amount", 200, "currency", "ETB",
                        "debitParty", "A1", "creditParty", "B1", "status", "SUCCESS",
                        "timestamp", "2026-09-04T10:00:00"),
                Map.of("transactionType", "SEND_MONEY", "amount", 300, "currency", "ETB",
                        "debitParty", "A2", "creditParty", "B2", "status", "PENDING",
                        "timestamp", "2026-09-04T10:01:00")
        );
        mvc.perform(post("/transactions")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(batch)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void recentTransactionsFromCache() throws Exception {
        String token = registerAndLogin("recentuser", "pass123");
        mvc.perform(get("/transactions/recent")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void cacheMaxSize() {
        TransactionCache testCache = new TransactionCache();
        for (int i = 0; i < 510; i++) {
            Transaction t = new Transaction();
            t.setTransactionType("DEPOSIT");
            t.setAmount(BigDecimal.ONE);
            t.setCurrency("ETB");
            t.setDebitParty("D");
            t.setCreditParty("C");
            t.setStatus("SUCCESS");
            t.setTimestamp(LocalDateTime.now());
            testCache.add(t);
        }
        assertThat(testCache.size()).isEqualTo(500);
    }

    @Test
    void cacheConcurrentAccess() throws Exception {
        TransactionCache testCache = new TransactionCache();
        ExecutorService executor = Executors.newFixedThreadPool(10);
        for (int i = 0; i < 100; i++) {
            executor.submit(() -> {
                Transaction t = new Transaction();
                t.setTransactionType("SEND_MONEY");
                t.setAmount(BigDecimal.TEN);
                t.setCurrency("ETB");
                t.setDebitParty("D");
                t.setCreditParty("C");
                t.setStatus("SUCCESS");
                t.setTimestamp(LocalDateTime.now());
                testCache.add(t);
            });
        }
        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);
        assertThat(testCache.size()).isEqualTo(100);
    }

    @Test
    void searchByDebitParty() throws Exception {
        String token = registerAndLogin("searchuser1", "pass123");
        mvc.perform(get("/transactions?debitParty=251700000001")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void searchByStatusAndType() throws Exception {
        String token = registerAndLogin("searchuser2", "pass123");
        mvc.perform(get("/transactions?transactionType=SEND_MONEY&status=SUCCESS")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void summaryEndpoint() throws Exception {
        String token = registerAndLogin("summaryuser", "pass123");
        mvc.perform(get("/transactions/summarys")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }
}
