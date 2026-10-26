package com.safaricom.mpesa_api.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transactions", indexes = {
    @Index(name = "idx_debit_party",       columnList = "debitParty"),
    @Index(name = "idx_credit_party",      columnList = "creditParty"),
    @Index(name = "idx_transaction_type",  columnList = "transactionType"),
    @Index(name = "idx_status",            columnList = "status"),
    @Index(name = "idx_category",          columnList = "category"),
    @Index(name = "idx_timestamp",         columnList = "timestamp")
})
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String transactionType;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(nullable = false, length = 10)
    private String currency;

    @Column(nullable = false)
    private String debitParty;

    @Column(nullable = false)
    private String creditParty;

    private String accountReference;

    private String category;

    @Column(nullable = false)
    private String status;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTransactionType() { return transactionType; }
    public void setTransactionType(String transactionType) { this.transactionType = transactionType; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public String getDebitParty() { return debitParty; }
    public void setDebitParty(String debitParty) { this.debitParty = debitParty; }

    public String getCreditParty() { return creditParty; }
    public void setCreditParty(String creditParty) { this.creditParty = creditParty; }

    public String getAccountReference() { return accountReference; }
    public void setAccountReference(String accountReference) { this.accountReference = accountReference; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}
