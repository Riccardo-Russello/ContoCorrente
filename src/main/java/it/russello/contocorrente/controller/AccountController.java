package it.russello.contocorrente.controller;

import it.russello.contocorrente.dto.AccountCreateRequest;
import it.russello.contocorrente.dto.AccountResponse;
import it.russello.contocorrente.dto.AccountUpdateRequest;
import it.russello.contocorrente.service.AccountService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/account")
public class AccountController {
    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @PostMapping
    public ResponseEntity<AccountResponse> create(
            @RequestBody AccountCreateRequest request){
        AccountResponse response = accountService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping
    public ResponseEntity<List<AccountResponse>> findAll(){
        return ResponseEntity.status(HttpStatus.OK).body(accountService.findAll());
    }
    @GetMapping("/{id}")
    public ResponseEntity<AccountResponse> findById(
            @PathVariable("id") Long id){
        return ResponseEntity.status(HttpStatus.OK).body(accountService.findById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AccountResponse> update(
            @PathVariable("id") Long id,
            @RequestBody AccountUpdateRequest request){
        return ResponseEntity.status(HttpStatus.OK).body(accountService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id")Long id){
        accountService.delete(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
