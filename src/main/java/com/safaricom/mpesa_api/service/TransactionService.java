package com.safaricom.mpesa_api.service;

import com.safaricom.mpesa_api.dto.TransactionRequest;
import com.safaricom.mpesa_api.exception.AppException;
import com.safaricom.mpesa_api.model.Transaction;
import com.safaricom.mpesa_api.repository.TransactionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class TransactionService {

    private static final Set<String> VALID_TYPES = Set.of(
            "DEPOSIT", "WITHDRAWAL", "SEND_MONEY", "MERCHANT_PAYMENT"
    );
    private static final Set<String> VALID_STATUSES = Set.of(
            "SUCCESS", "FAILED", "PENDING"
    );

    private final TransactionRepository repository;
    private final TransactionCache cache;

    public TransactionService(TransactionRepository repository, TransactionCache cache) {
        this.repository = repository;
        this.cache = cache;
    }

    public Transaction create(TransactionRequest req) {
        validate(req);
        Transaction t = toEntity(req);
        Transaction saved = repository.save(t);
        cache.add(saved);
        return saved;
    }

    public List<Transaction> createBatch(List<TransactionRequest> requests) {
        requests.forEach(this::validate);
        List<Transaction> entities = requests.stream().map(this::toEntity).collect(Collectors.toList());
        List<Transaction> saved = repository.saveAll(entities);
        cache.addAll(saved);
        return saved;
    }

    public List<Transaction> search(String transactionType, String category, String status,
                                    String debitParty, String creditParty,
                                    LocalDateTime from, LocalDateTime to) {
        List<Transaction> cached = cache.getAll();

        boolean canUseCache = cached.size() < 500 ||
                (from == null && to == null && transactionType == null &&
                 category == null && status == null && debitParty == null && creditParty == null);

        if (!cached.isEmpty()) {
            List<Transaction> filtered = cached.stream()
                    .filter(t -> transactionType == null || t.getTransactionType().equals(transactionType))
                    .filter(t -> category == null || category.equals(t.getCategory()))
                    .filter(t -> status == null || t.getStatus().equals(status))
                    .filter(t -> debitParty == null || t.getDebitParty().equals(debitParty))
                    .filter(t -> creditParty == null || t.getCreditParty().equals(creditParty))
                    .filter(t -> from == null || !t.getTimestamp().isBefore(from))
                    .filter(t -> to == null || !t.getTimestamp().isAfter(to))
                    .collect(Collectors.toList());

            if (!filtered.isEmpty() || canUseCache) {
                return filtered;
            }
        }

        return repository.search(transactionType, category, status, debitParty, creditParty, from, to);
    }

    public List<Transaction> getRecent() {
        return cache.getRecent(20);
    }

    public Map<String, Long> getSummary(String category) {
        List<Transaction> transactions = cache.getAll();
        return transactions.stream()
                .filter(t -> category == null || category.equals(t.getCategory()))
                .collect(Collectors.groupingBy(Transaction::getTransactionType, Collectors.counting()));
    }

    private void validate(TransactionRequest req) {
        if (req.getAmount() == null || req.getAmount().signum() <= 0) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Transaction amount must be greater than zero");
        }
        if (!VALID_TYPES.contains(req.getTransactionType())) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Invalid transaction type: " + req.getTransactionType());
        }
        if (!VALID_STATUSES.contains(req.getStatus())) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Invalid transaction status: " + req.getStatus());
        }
    }

    private Transaction toEntity(TransactionRequest req) {
        Transaction t = new Transaction();
        t.setTransactionType(req.getTransactionType());
        t.setAmount(req.getAmount());
        t.setCurrency(req.getCurrency());
        t.setDebitParty(req.getDebitParty());
        t.setCreditParty(req.getCreditParty());
        t.setAccountReference(req.getAccountReference());
        t.setCategory(req.getCategory());
        t.setStatus(req.getStatus());
        t.setTimestamp(req.getTimestamp() != null ? req.getTimestamp() : LocalDateTime.now());
        return t;
    }
}
