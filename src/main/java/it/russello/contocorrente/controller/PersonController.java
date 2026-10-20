package it.russello.contocorrente.controller;

import it.russello.contocorrente.dto.PersonRequest;
import it.russello.contocorrente.dto.PersonResponse;
import it.russello.contocorrente.service.PersonService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;

@Tag(name = "Persone", description = "Gestione delle persone")
@RestController
@RequestMapping("/api/person")
@RequiredArgsConstructor
public class PersonController {
    private final PersonService personService;

    @Operation(summary = "Crea una persona")
    @PostMapping
    public ResponseEntity<PersonResponse> create(@RequestBody PersonRequest request) {
        PersonResponse response = personService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }


    @Operation(summary = "Elenca tutte le persone")
    @GetMapping
    public ResponseEntity<List<PersonResponse>> findAll() {
        return ResponseEntity.status(HttpStatus.OK).body(personService.findAll());
    }

    @Operation(summary = "Cerca una persona per ID")
    @GetMapping("/{id}")
    public ResponseEntity<PersonResponse> findById(@PathVariable("id") Long id) {
        return ResponseEntity.status(HttpStatus.OK).body(personService.findById(id));
    }

    @Operation(summary = "Aggiorna una persona")
    @PutMapping("/{id}")
    public ResponseEntity<PersonResponse> update(@PathVariable("id") Long id,
                                                 @RequestBody PersonRequest request) {
        return ResponseEntity.status(HttpStatus.OK).body(personService.update(id, request));
    }

    @Operation(summary = "Elimina una persona")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") Long id) {
        personService.delete(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
