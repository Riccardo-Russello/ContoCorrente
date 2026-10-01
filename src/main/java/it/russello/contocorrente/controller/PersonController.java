package it.russello.contocorrente.controller;

import it.russello.contocorrente.dto.PersonRequest;
import it.russello.contocorrente.dto.PersonResponse;
import it.russello.contocorrente.service.PersonService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/person")
public class PersonController {
    private final PersonService personService;
    public PersonController(PersonService personService) {
        this.personService = personService;
    }

    // POST
    @PostMapping
    public ResponseEntity<PersonResponse> create(@RequestBody PersonRequest request){
        PersonResponse response = personService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // GET
    @GetMapping
    public ResponseEntity<List<PersonResponse>> findAll(){
        return ResponseEntity.status(HttpStatus.OK).body(personService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<PersonResponse> findById(@PathVariable("id") Long id){
        return ResponseEntity.status(HttpStatus.OK).body(personService.findById(id));
    }

    // PUT
    @PutMapping("/{id}")
    public ResponseEntity<PersonResponse> update(@PathVariable("id") Long id,
                                                 @RequestBody PersonRequest request){
        return ResponseEntity.status(HttpStatus.OK).body(personService.update(id, request));
    }

    // DELETE
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") Long id){
        personService.delete(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
