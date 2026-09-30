package it.russello.contocorrente.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Entity
@Table(name = "accounttransaction")
public class AccountTransaction {
    // === ATTRIBUTI ===
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)     // Rende l'id auto incrementale
    private Long id;

    // Più movimenti appartengono allo stesso conto
    @ManyToOne(fetch = FetchType.LAZY, optional = false)    // LAZY indica a JPA di rimandare il caricamento del conto associato al movimento finché non serve
    @JoinColumn(name = "account_id",
            nullable = false,
            updatable = false,
            foreignKey = @ForeignKey(name = "fk_transaction_account")
    )
    private Account account;

    @Enumerated(EnumType.STRING)                // Conserva il tipo come String
    @Column(nullable = false, length = 10, columnDefinition = "VARCHAR(10)")      // Not null e lunghezza = 10
    private TransactionType type;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(name = "occurred_at", nullable = false, columnDefinition = "DATETIME(6)")
    private LocalDateTime occurredAt;

    // === COSTRUTTORI ===
    protected AccountTransaction(){}     // Costruttore richiesto da JPA

    public AccountTransaction(Account account, TransactionType type, BigDecimal amount){
        this.account = account;
        this.type = type;
        this.amount = amount;
        this.occurredAt = LocalDateTime.now()
                .truncatedTo(ChronoUnit.MICROS);    // Mantiene la precisione fino ai microsecondi (es. 2026-09-25T11:13:45.123456)
    }

    // === GETTER ===
    public Long getId() {
        return id;
    }
    public Account getAccount() {
        return account;
    }
    public TransactionType getType() {
        return type;
    }
    public BigDecimal getAmount() {
        return amount;
    }
    public LocalDateTime getOccurredAt() {
        return occurredAt;
    }
}
