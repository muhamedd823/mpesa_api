package com.safaricom.mpesa_api.controller;

import com.safaricom.mpesa_api.dto.TransactionRequest;
import com.safaricom.mpesa_api.model.Transaction;
import com.safaricom.mpesa_api.service.TransactionService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/transactions")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody Object body) {
        if (body instanceof List) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> rawList = (List<Map<String, Object>>) body;
            List<TransactionRequest> requests = rawList.stream()
                    .map(this::mapToRequest)
                    .toList();
            requests.forEach(this::validateRequest);
            List<Transaction> saved = transactionService.createBatch(requests);
            return ResponseEntity.status(HttpStatus.CREATED).body(saved);
        } else {
            @SuppressWarnings("unchecked")
            Map<String, Object> raw = (Map<String, Object>) body;
            TransactionRequest req = mapToRequest(raw);
            validateRequest(req);
            Transaction saved = transactionService.create(req);
            return ResponseEntity.status(HttpStatus.CREATED).body(saved);
        }
    }

    @GetMapping
    public ResponseEntity<List<Transaction>> search(
            @RequestParam(required = false) String transactionType,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String debitParty,
            @RequestParam(required = false) String creditParty,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {

        List<Transaction> results = transactionService.search(
                transactionType, category, status, debitParty, creditParty, from, to);
        return ResponseEntity.ok(results);
    }

    @GetMapping("/recent")
    public ResponseEntity<List<Transaction>> recent() {
        return ResponseEntity.ok(transactionService.getRecent());
    }

    @GetMapping("/summarys")
    public ResponseEntity<Map<String, Long>> summary(@RequestParam(required = false) String category) {
        return ResponseEntity.ok(transactionService.getSummary(category));
    }

    private TransactionRequest mapToRequest(Map<String, Object> raw) {
        TransactionRequest req = new TransactionRequest();
        req.setTransactionType((String) raw.get("transactionType"));
        req.setCurrency((String) raw.get("currency"));
        req.setDebitParty((String) raw.get("debitParty"));
        req.setCreditParty((String) raw.get("creditParty"));
        req.setAccountReference((String) raw.get("accountReference"));
        req.setCategory((String) raw.get("category"));
        req.setStatus((String) raw.get("status"));
        if (raw.get("amount") != null) {
            req.setAmount(new java.math.BigDecimal(raw.get("amount").toString()));
        }
        if (raw.get("timestamp") != null) {
            req.setTimestamp(LocalDateTime.parse(raw.get("timestamp").toString()));
        }
        return req;
    }

    private void validateRequest(TransactionRequest req) {
        if (req.getTransactionType() == null || req.getTransactionType().isBlank()) {
            throw new com.safaricom.mpesa_api.exception.AppException(
                    org.springframework.http.HttpStatus.BAD_REQUEST, "Transaction type is required");
        }
        if (req.getAmount() == null) {
            throw new com.safaricom.mpesa_api.exception.AppException(
                    org.springframework.http.HttpStatus.BAD_REQUEST, "Amount is required");
        }
        if (req.getCurrency() == null || req.getCurrency().isBlank()) {
            throw new com.safaricom.mpesa_api.exception.AppException(
                    org.springframework.http.HttpStatus.BAD_REQUEST, "Currency is required");
        }
        if (req.getDebitParty() == null || req.getDebitParty().isBlank()) {
            throw new com.safaricom.mpesa_api.exception.AppException(
                    org.springframework.http.HttpStatus.BAD_REQUEST, "Debit party is required");
        }
        if (req.getCreditParty() == null || req.getCreditParty().isBlank()) {
            throw new com.safaricom.mpesa_api.exception.AppException(
                    org.springframework.http.HttpStatus.BAD_REQUEST, "Credit party is required");
        }
        if (req.getStatus() == null || req.getStatus().isBlank()) {
            throw new com.safaricom.mpesa_api.exception.AppException(
                    org.springframework.http.HttpStatus.BAD_REQUEST, "Status is required");
        }
    }
}
