package it.russello.contocorrente.controller;

import it.russello.contocorrente.dto.AccountTransactionRequest;
import it.russello.contocorrente.dto.AccountTransactionRequest.TransferRequest;
import it.russello.contocorrente.dto.AccountTransactionResponse;
import it.russello.contocorrente.dto.AccountTransactionResponse.TransferResponse;
import it.russello.contocorrente.dto.TransactionListResponse;
import it.russello.contocorrente.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.apache.coyote.Response;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/account/{id}/transaction")
@RequiredArgsConstructor
public class TransactionController {
    private final TransactionService transactionService;

    @PostMapping
    public ResponseEntity<AccountTransactionResponse> createTransaction(
            @PathVariable("id") Long accountId,
            @RequestBody AccountTransactionRequest request) {
        AccountTransactionResponse response = transactionService.createTransaction(accountId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/transfer")
    public ResponseEntity<TransferResponse> transfer(
            @PathVariable("id") Long sourceAccountId,
            @RequestBody TransferRequest request){
        TransferResponse response = transactionService.transfer(sourceAccountId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<TransactionListResponse>>
            getLastFiveTransactions(@PathVariable("id") Long accountId){
        return ResponseEntity.status(HttpStatus.OK).body(transactionService.getLastFiveTransactions(accountId));
    }
}
