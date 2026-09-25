package it.russello.contocorrente.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "conto")
public class Conto {
    // === ATTRIBUTI ===
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)     // Rende l'attributo auto incrementale
    private Long id;

    @Column(name = "numero_conto", nullable = false, unique = true, length = 30)
    private String numeroConto;

    @Column(name = "data_apertura", nullable = false, columnDefinition ="DATETIME")
    private LocalDateTime dataApertura;      // senza frazioni di secondo

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal saldo;

    // === COSTRUTTORI ===
    protected Conto(){}     // Costruttore richiesto da JPA

    public Conto(String numeroConto){
        this.numeroConto = numeroConto;
        this.dataApertura = LocalDateTime.now().withNano(0);    // Imposta i nanosecondi a 0
        this.saldo = new BigDecimal("0.00");
    }

    // === GETTER ===
    public Long getId() {
        return id;
    }
    public String getNumeroConto() {
        return numeroConto;
    }
    public LocalDateTime getDataApertura() {
        return dataApertura;
    }
    public BigDecimal getSaldo() {
        return saldo;
    }

    // === SETTER ===
    public void setSaldo(BigDecimal saldo) {
        this.saldo = saldo;
    }
}
