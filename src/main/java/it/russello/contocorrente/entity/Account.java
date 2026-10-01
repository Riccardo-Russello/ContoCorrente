package it.russello.contocorrente.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "account")
public class Account {
    // === ATTRIBUTI ===
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)     // Rende l'attributo auto incrementale
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "person_id",
            nullable = false,
            updatable = false,
            foreignKey = @ForeignKey(name = "fk_account_person"))
    private Person person;

    @Column(name = "account_number", nullable = false, unique = true, length = 30)
    private String accountNumber;

    @Column(name = "opened_at", nullable = false, updatable = false, columnDefinition ="DATETIME")
    private LocalDateTime openedAt;      // senza frazioni di secondo

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal balance;

    // === COSTRUTTORI ===
    protected Account(){}     // Costruttore richiesto da JPA

    public Account(Person person, String accountNumber){
        this.person = person;
        this.accountNumber = accountNumber;
        this.openedAt = LocalDateTime.now().withNano(0);    // Imposta i nanosecondi a 0
        this.balance = new BigDecimal("0.00");
    }

    // === GETTER ===
    public Long getId() {
        return id;
    }
    public Person getPerson() {return person;}
    public String getAccountNumber() {
        return accountNumber;
    }
    public LocalDateTime getOpenedAt() {
        return openedAt;
    }
    public BigDecimal getBalance() {
        return balance;
    }

    // === SETTER ===
    public void setSaldo(BigDecimal balance) {
        this.balance = balance;
    }
    public void setBalance(BigDecimal balance){     // Da usare nel Service per versamenti e prelievi
        this.balance = balance;
    }
}
