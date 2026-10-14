package it.russello.contocorrente.service;

import it.russello.contocorrente.dto.AccountBalanceResponse;
import it.russello.contocorrente.dto.AccountCreateRequest;
import it.russello.contocorrente.dto.AccountResponse;
import it.russello.contocorrente.dto.AccountUpdateRequest;
import it.russello.contocorrente.entity.Account;
import it.russello.contocorrente.entity.Person;
import it.russello.contocorrente.exception.ApiException;
import it.russello.contocorrente.repository.AccountRepository;
import it.russello.contocorrente.repository.AccountTransactionRepository;
import it.russello.contocorrente.repository.PersonRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AccountServiceTest {
    private static final LocalDateTime OPENED_AT = LocalDateTime.of(2026, 1, 10, 12, 0);
    @Mock
    private AccountRepository accountRepository;
    @Mock
    private PersonRepository personRepository;
    @Mock
    private AccountTransactionRepository accountTransactionRepository;
    @InjectMocks
    private AccountService accountService;

    private static Person person(){
        Person person = new Person("Mario", "Rossi", LocalDate.of(1990, 5, 15));
        ReflectionTestUtils.setField(person,"id",10L);
        return person;
    }
    private static Account account(Long id){
        Account account = new Account(person(), "ACC-001");
        ReflectionTestUtils.setField(account,"id",id);
        ReflectionTestUtils.setField(account,"openedAt",OPENED_AT);
        return account;
    }
    private static AccountCreateRequest validCreateRequest(){
        return new AccountCreateRequest(10L, "ACC-001");
    }

    @Test
    void createAccount(){
        Person owner = person();
        Account saved = account(1L);

        when(personRepository.findById(10L)).thenReturn(Optional.of(owner));
        when(accountRepository.existsByAccountNumber("ACC-001")).thenReturn(false);
        when(accountRepository.saveAndFlush(any(Account.class))).thenReturn(saved);
        AccountResponse response = accountService.create(validCreateRequest());
        assertEquals(new AccountResponse(1L, 10L, "ACC-001", OPENED_AT, new BigDecimal("0.00")), response);
        ArgumentCaptor<Account> captor = ArgumentCaptor.forClass(Account.class);
        verify(accountRepository).saveAndFlush(captor.capture());

        Account submitted = captor.getValue();
        assertNull(submitted.getId());
        assertSame(owner, submitted.getPerson());
        assertEquals("ACC-001", submitted.getAccountNumber());
        assertEquals(new BigDecimal("0.00"), submitted.getBalance());
        assertNotNull(submitted.getOpenedAt());
        assertEquals(0, submitted.getOpenedAt().getNano());

        verifyNoInteractions(accountTransactionRepository);
    }

    @ParameterizedTest
    @MethodSource("invalidCreateRequests")
    void createWithInvalidData(AccountCreateRequest request){
        ApiException exception = assertThrows(ApiException.class, () -> accountService.create(request));
        assertApiError(exception, HttpStatus.BAD_REQUEST, "INVALID_ACCOUNT_DATA");
        verifyNoInteractions(accountRepository,  personRepository, accountTransactionRepository);
    }

    @Test
    void createMissingPerson(){
        when(personRepository.findById(10L)).thenReturn(Optional.empty());
        ApiException exception = assertThrows(ApiException.class, () -> accountService.create(validCreateRequest()));
        assertApiError(exception, HttpStatus.NOT_FOUND, "PERSON_NOT_FOUND");
        verifyNoInteractions(accountRepository, accountTransactionRepository);
    }

    @Test
    void createDuplicateAccountNumber(){
        when(personRepository.findById(10L)).thenReturn(Optional.of(person()));
        when(accountRepository.existsByAccountNumber("ACC-001")).thenReturn(true);
        ApiException exception = assertThrows(ApiException.class, () -> accountService.create(validCreateRequest()));
        assertApiError(exception, HttpStatus.CONFLICT, "ACCOUNT_NUMBER_ALREADY_EXISTS");
        verify(accountRepository, never()).saveAndFlush(any(Account.class));
        verifyNoInteractions(accountTransactionRepository);
    }

    @Test
    void createAccountNumberWith30Characters(){
        String number = "a".repeat(30);
        when(personRepository.findById(10L)).thenReturn(Optional.of(person()));
        when(accountRepository.saveAndFlush(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));
        AccountResponse response = accountService.create(new AccountCreateRequest(10L, number));
        assertEquals(number, response.accountNumber());
        verify(accountRepository).existsByAccountNumber(number);
        verify(accountRepository).saveAndFlush(any(Account.class));
    }

    @Test
    void findAllAccounts(){
        Account first = account(1L);
        Account second = account(2L);
        second.setAccountNumber("ACC-002");
        second.setBalance(new BigDecimal("50.00"));
        when(accountRepository.findAll()).thenReturn(List.of(first,second));
        assertEquals(List.of(new AccountResponse(1L, 10L, "ACC-001", OPENED_AT, new BigDecimal("0.00")),
                new AccountResponse(2L, 10L, "ACC-002", OPENED_AT, new BigDecimal("50.00"))),
                accountService.findAll());
        verify(accountRepository).findAll();
    }

    @Test
    void findById(){
        when(accountRepository.findById(1L)).thenReturn(Optional.of(account(1L)));
        assertEquals(new AccountResponse(1L, 10L, "ACC-001", OPENED_AT, new BigDecimal("0.00")),
                accountService.findById(1L));
        verify(accountRepository).findById(1L);
    }

    @Test
    void getBalanceAndCurrency(){
        Account existing = account(1L);
        existing.setBalance(new BigDecimal("125.50"));
        when(accountRepository.findById(1L)).thenReturn(Optional.of(existing));
        assertEquals(new AccountBalanceResponse(1L, new BigDecimal("125.50"), "€ (EUR)"),
                accountService.getBalance(1L));
        verify(accountRepository).findById(1L);
    }

    @Test
    void updateExistingAccount(){
        Account existing = account(1L);
        existing.setBalance(new BigDecimal("50.00"));
        Person originalOwner = existing.getPerson();

        when(accountRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(accountRepository.existsByAccountNumberAndIdNot("ACC-002", 1L)).thenReturn(false);
        when(accountRepository.saveAndFlush(existing)).thenReturn(existing);

        AccountResponse response = accountService.update(1L, new AccountUpdateRequest("ACC-002"));
        assertEquals(new AccountResponse(1L, 10L, "ACC-002", OPENED_AT, new BigDecimal("50.00")),
                response);
        assertEquals("ACC-002", existing.getAccountNumber());
        assertSame(originalOwner, existing.getPerson());
        assertEquals(OPENED_AT, existing.getOpenedAt());
        assertEquals(new BigDecimal("50.00"), existing.getBalance());
        verify(accountRepository).existsByAccountNumberAndIdNot("ACC-002", 1L);
        verify(accountRepository).saveAndFlush(existing);
        verifyNoInteractions(personRepository, accountTransactionRepository);
    }

    @ParameterizedTest
    @MethodSource("invalidUpdateRequests")
    void updateInvalidData(AccountUpdateRequest request){
        Account existing = account(1L);
        when(accountRepository.findById(1L)).thenReturn(Optional.of(existing));
        ApiException exception = assertThrows(ApiException.class, () -> accountService.update(1L, request));
        assertApiError(exception, HttpStatus.BAD_REQUEST, "INVALID_ACCOUNT_DATA");
        assertEquals("ACC-001",  existing.getAccountNumber());
        verify(accountRepository, never()).saveAndFlush(any(Account.class));
        verify(accountRepository).findById(1L);
        verifyNoMoreInteractions(accountRepository);
        verifyNoInteractions(personRepository, accountTransactionRepository);
    }

    @Test
    void updateDuplicateAccountNumber(){
        Account existing = account(1L);
        when(accountRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(accountRepository.existsByAccountNumberAndIdNot("ACC-002", 1L)).thenReturn(true);
        ApiException exception = assertThrows(ApiException.class, () -> accountService.update(1L, new AccountUpdateRequest("ACC-002")));
        assertApiError(exception, HttpStatus.CONFLICT, "ACCOUNT_NUMBER_ALREADY_EXISTS");
        assertEquals("ACC-001", existing.getAccountNumber());
        verify(accountRepository, never()).saveAndFlush(any(Account.class));
    }

    @Test
    void updateAccountNumberWith30Characters(){
        String number = "a".repeat(30);
        Account existing = account(1L);
        existing.setAccountNumber(number);
        when(accountRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(accountRepository.existsByAccountNumberAndIdNot(number, 1L)).thenReturn(false);
        when(accountRepository.saveAndFlush(existing)).thenReturn(existing);
        AccountResponse response = accountService.update(1L, new AccountUpdateRequest(number));
        assertEquals(number, response.accountNumber());
        verify(accountRepository).existsByAccountNumberAndIdNot(number, 1L);
        verify(accountRepository).saveAndFlush(existing);
    }

    @Test
    void deleteValidAccount(){
        Account existing = account(1L);
        when(accountRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(accountTransactionRepository.existsByAccount_Id(1L)).thenReturn(false);
        accountService.delete(1L);
        var order = inOrder(accountRepository, accountTransactionRepository);
        order.verify(accountRepository).findById(1L);
        order.verify(accountTransactionRepository).existsByAccount_Id(1L);
        order.verify(accountRepository).delete(existing);
        order.verify(accountRepository).flush();
        verifyNoInteractions(personRepository);
    }

    @ParameterizedTest
    @ValueSource(strings = {"10.00", "-10.00"})
    void deleteAccountWithBalance(String balance){
        Account existing = account(1L);
        existing.setBalance(new BigDecimal(balance));
        when(accountRepository.findById(1L)).thenReturn(Optional.of(existing));
        ApiException exception = assertThrows(ApiException.class, () -> accountService.delete(1L));
        assertApiError(exception, HttpStatus.CONFLICT, "ACCOUNT_BALANCE_NOT_ZERO");
        verify(accountRepository, never()).delete(any(Account.class));
        verify(accountRepository, never()).flush();
        verifyNoInteractions(personRepository, accountTransactionRepository);
    }

    @Test
    void deleteAccountWithTransactions(){
        when(accountRepository.findById(1L)).thenReturn(Optional.of(account(1L)));
        when(accountTransactionRepository.existsByAccount_Id(1L)).thenReturn(true);
        ApiException exception = assertThrows(ApiException.class, () -> accountService.delete(1L));
        assertApiError(exception, HttpStatus.CONFLICT, "ACCOUNT_HAS_TRANSACTION");
        verify(accountRepository, never()).delete(any(Account.class));
        verify(accountRepository, never()).flush();
    }

    @ParameterizedTest
    @ValueSource(strings = {"findById", "getBalance", "update", "delete"})
    void operationsMissingAccount(String operation){
        when(accountRepository.findById(1L)).thenReturn(Optional.empty());
        ApiException exception = assertThrows(ApiException.class, () -> {
            switch(operation){
                case "findById" -> accountService.findById(1L);
                case "getBalance" -> accountService.getBalance(1L);
                case "update" -> accountService.update(1L, new AccountUpdateRequest("ACC-002"));
                case "delete" -> accountService.delete(1L);
                default -> throw new IllegalArgumentException(operation);
            }
        });
        assertApiError(exception, HttpStatus.NOT_FOUND, "ACCOUNT_NOT_FOUND");
        verify(accountRepository).findById(1L);
        verifyNoMoreInteractions(accountRepository);
        verifyNoInteractions(personRepository, accountTransactionRepository);
    }


    private static void assertApiError(ApiException exception, HttpStatus status, String code){
        assertEquals(status, exception.getStatus());
        assertEquals(code, exception.getCode());
    }
    static Stream<AccountCreateRequest> invalidCreateRequests(){
        return Stream.of(
                (AccountCreateRequest) null,
                new AccountCreateRequest(null, "ACC-001"),
                new AccountCreateRequest(10L, null),
                new AccountCreateRequest(10L, ""),
                new AccountCreateRequest(10L, "    "),
                new AccountCreateRequest(10L, "a".repeat(31))
        );
    }
    static Stream<AccountUpdateRequest> invalidUpdateRequests(){
        return Stream.of(
                (AccountUpdateRequest) null,
                new AccountUpdateRequest(null),
                new AccountUpdateRequest(""),
                new AccountUpdateRequest("    "),
                new AccountUpdateRequest("a".repeat(31))
        );
    }
}
