package it.russello.contocorrente.entity;

import jakarta.persistence.*;

import java.time.LocalDate;

@Entity
@Table(name = "person")
public class Person {
    // ===== ATTRIBUTI =====
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "first_name", nullable = false, length = 30)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 30)
    private String lastName;

    @Column(name = "date_of_birth", nullable = false)
    private LocalDate dateOfBirth;      // Rappresenta la data senza l'orario

    // ===== COSTRUTTORI =====
    protected Person() {}   // Richiesto da JPA
    public Person(String firstName, String lastName,
                  LocalDate dateOfBirth) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.dateOfBirth = dateOfBirth;
    }

    // ===== GETTER =====
    public Long getId() {
        return id;
    }
    public String getFirstName() {
        return firstName;
    }
    public String getLastName() {
        return lastName;
    }
    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    // ===== SETTER =====
    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }
    public void setLastName(String lastName) {
        this.lastName = lastName;
    }
    public void setDateOfBirth(LocalDate dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }
}
