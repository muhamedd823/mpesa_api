package com.safaricom.mpesa_api.repository;

import com.safaricom.mpesa_api.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    @Query("SELECT t FROM Transaction t WHERE " +
           "(:transactionType IS NULL OR t.transactionType = :transactionType) AND " +
           "(:category       IS NULL OR t.category       = :category)       AND " +
           "(:status         IS NULL OR t.status         = :status)         AND " +
           "(:debitParty     IS NULL OR t.debitParty     = :debitParty)     AND " +
           "(:creditParty    IS NULL OR t.creditParty    = :creditParty)    AND " +
           "(:from           IS NULL OR t.timestamp     >= :from)           AND " +
           "(:to             IS NULL OR t.timestamp     <= :to)             " +
           "ORDER BY t.timestamp DESC")
    List<Transaction> search(
        @Param("transactionType") String transactionType,
        @Param("category")        String category,
        @Param("status")          String status,
        @Param("debitParty")      String debitParty,
        @Param("creditParty")     String creditParty,
        @Param("from")            LocalDateTime from,
        @Param("to")              LocalDateTime to
    );
}
