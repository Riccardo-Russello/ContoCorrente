package it.russello.contocorrente.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Entity
@Table(name = "movimento")
public class Movimento {
    // === ATTRIBUTI ===
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)     // Rende l'id auto incrementale
    private Long id;

    // Più movimenti appartengono allo stesso conto
    @ManyToOne(fetch = FetchType.LAZY, optional = false)    // LAZY indica a JPA di rimandare il caricamento del conto associato al movimento finché non serve
    @JoinColumn(name = "id_conto", nullable = false)        // Associa il Java conto alla colonna SQL id_conto
    private Conto conto;

    @Enumerated(EnumType.STRING)                // Conserva il tipo come String
    @Column(nullable = false, length = 10)      // Not null e lunghezza = 10
    private TipoMovimento tipo;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal importo;

    @Column(name = "data_transazione", nullable = false, columnDefinition = "DATETIME(6)")
    private LocalDateTime dataTransazione;

    // === COSTRUTTORI ===
    protected Movimento(){}     // Costruttore richiesto da JPA

    public Movimento(Conto conto, TipoMovimento tipo, BigDecimal importo){
        this.conto = conto;
        this.tipo = tipo;
        this.importo = importo;
        this.dataTransazione = LocalDateTime.now()
                .truncatedTo(ChronoUnit.MICROS);    // Mantiene la precisione fino ai microsecondi (es. 2026-09-25T11:13:45.123456)
    }

    // === GETTER ===
    public Long getId() {
        return id;
    }
    public Conto getConto() {
        return conto;
    }
    public TipoMovimento getTipo() {
        return tipo;
    }
    public BigDecimal getImporto() {
        return importo;
    }
    public LocalDateTime getDataTransazione() {
        return dataTransazione;
    }
}
