package com.pecunia.account.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.pecunia.account.application.exception.AccountNotFoundException;
import com.pecunia.account.application.port.in.ArchiveAccount;
import com.pecunia.account.application.port.in.ArchiveAccountCommand;
import com.pecunia.account.application.port.in.GetAccount;
import com.pecunia.account.application.port.in.GetAccountQuery;
import com.pecunia.account.application.port.in.ListAccounts;
import com.pecunia.account.application.port.in.OpenAccount;
import com.pecunia.account.application.port.in.OpenAccountCommand;
import com.pecunia.account.application.readmodel.AccountView;
import com.pecunia.account.domain.AccountStatus;
import com.pecunia.account.domain.AccountType;
import com.pecunia.account.domain.Iban;
import com.pecunia.account.domain.exception.AccountAlreadyArchivedException;
import com.pecunia.account.web.mapper.AccountMapperImpl;
import com.pecunia.sharedinfra.security.PecuniaOidcUserService;
import com.pecunia.sharedinfra.security.SecurityConfig;
import com.pecunia.sharedkernel.AccountId;
import com.pecunia.sharedkernel.CurrentUserProvider;
import com.pecunia.sharedkernel.Money;
import com.pecunia.sharedkernel.UserId;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Web-slice tests for {@link AccountController}. The real {@link AccountMapperImpl} is imported (not
 * mocked) so the DTO/command mapping is exercised end to end; the driving ports are mocked. The
 * {@code account}-scoped {@code @RestControllerAdvice} is auto-detected by {@code @WebMvcTest}, so
 * the 400/404/409 mappings are covered here too.
 */
@WebMvcTest(AccountController.class)
@Import({SecurityConfig.class, AccountMapperImpl.class})
class AccountControllerTest {

    private static final UUID OWNER_ID = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000001");
    private static final UserId OWNER = UserId.of(OWNER_ID);
    private static final UUID ACCOUNT_ID = UUID.fromString("bbbbbbbb-0000-0000-0000-000000000002");
    private static final UUID CREDIT_CARD_ID = UUID.fromString("cccccccc-0000-0000-0000-000000000003");
    private static final String IBAN = "CH9300762011623852957";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OpenAccount openAccount;

    @MockitoBean
    private GetAccount getAccount;

    @MockitoBean
    private ListAccounts listAccounts;

    @MockitoBean
    private ArchiveAccount archiveAccount;

    @MockitoBean
    private CurrentUserProvider currentUserProvider;

    /** Keeps the slice hermetic: prevents a real OIDC discovery call to Keycloak at context load. */
    @MockitoBean
    private ClientRegistrationRepository clientRegistrationRepository;

    /** Referenced by SecurityConfig's OAuth2 login wiring; not exercised by this slice. */
    @MockitoBean
    private PecuniaOidcUserService pecuniaOidcUserService;

    private static AccountView currentAccountView() {
        return new AccountView(
                AccountId.of(ACCOUNT_ID),
                AccountType.CURRENT,
                "UBS Current",
                Optional.of(new Iban(IBAN)),
                AccountStatus.ACTIVE,
                Money.chf(new BigDecimal("1500.00")));
    }

    private static AccountView creditCardView() {
        return new AccountView(
                AccountId.of(CREDIT_CARD_ID),
                AccountType.CREDIT_CARD,
                "UBS Visa",
                Optional.empty(),
                AccountStatus.ACTIVE,
                Money.chf(new BigDecimal("0.00")));
    }

    @Test
    @DisplayName("POST registers an account and returns 201 with a Location header and the created body")
    void openAccountReturnsCreated() throws Exception {
        when(currentUserProvider.currentUserId()).thenReturn(OWNER);
        when(openAccount.open(any())).thenReturn(AccountId.of(ACCOUNT_ID));
        when(getAccount.getById(new GetAccountQuery(OWNER, AccountId.of(ACCOUNT_ID))))
                .thenReturn(currentAccountView());

        String body = """
                {"type":"CURRENT","name":"UBS Current","iban":"CH9300762011623852957",
                 "initialBalance":{"amount":"1500.00","currency":"CHF"}}""";

        mockMvc.perform(post("/api/accounts")
                        .with(oidcLogin())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", endsWith("/api/accounts/" + ACCOUNT_ID)))
                .andExpect(jsonPath("$.id").value(ACCOUNT_ID.toString()))
                .andExpect(jsonPath("$.type").value("CURRENT"))
                .andExpect(jsonPath("$.name").value("UBS Current"))
                .andExpect(jsonPath("$.iban").value(IBAN))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                // Money is normalised to scale 4 by the domain, so "1500.00" comes back as "1500.0000".
                .andExpect(jsonPath("$.initialBalance.amount").value("1500.0000"))
                .andExpect(jsonPath("$.initialBalance.currency").value("CHF"));

        ArgumentCaptor<OpenAccountCommand> captor = ArgumentCaptor.forClass(OpenAccountCommand.class);
        verify(openAccount).open(captor.capture());
        OpenAccountCommand command = captor.getValue();
        assertThat(command.owner()).isEqualTo(OWNER);
        assertThat(command.type()).isEqualTo(AccountType.CURRENT);
        assertThat(command.name()).isEqualTo("UBS Current");
        assertThat(command.iban()).contains(new Iban(IBAN));
        assertThat(command.initialBalance()).isEqualTo(Money.chf(new BigDecimal("1500.00")));
    }

    @Test
    @DisplayName("POST with a blank name is rejected by bean validation with 400 before reaching the use case")
    void openAccountRejectsBlankName() throws Exception {
        String body = """
                {"type":"CURRENT","name":"","initialBalance":{"amount":"1500.00","currency":"CHF"}}""";

        mockMvc.perform(post("/api/accounts")
                        .with(oidcLogin())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));

        verifyNoInteractions(openAccount);
    }

    private static Stream<String> invalidPayloads() {
        String tooLongName = "A".repeat(101);
        String tooLongIban = "CH93" + "0".repeat(31); // 35 chars, exceeds @Size(max = 34)
        return Stream.of(
                // missing type (@NotNull)
                """
                {"name":"UBS Current","initialBalance":{"amount":"1500.00","currency":"CHF"}}""",
                // name longer than 100 chars (@Size max)
                "{\"type\":\"CURRENT\",\"name\":\"" + tooLongName
                        + "\",\"initialBalance\":{\"amount\":\"1500.00\",\"currency\":\"CHF\"}}",
                // iban longer than 34 chars (@Size max)
                "{\"type\":\"CURRENT\",\"name\":\"UBS Current\",\"iban\":\"" + tooLongIban
                        + "\",\"initialBalance\":{\"amount\":\"1500.00\",\"currency\":\"CHF\"}}",
                // missing initialBalance (@NotNull)
                """
                {"type":"CURRENT","name":"UBS Current"}""",
                // amount with more than 4 decimals (@Pattern on the nested Money, reached via @Valid cascade)
                """
                {"type":"CURRENT","name":"UBS Current","initialBalance":{"amount":"12.345678","currency":"CHF"}}""",
                // currency not three uppercase letters (@Pattern on the nested Money)
                """
                {"type":"CURRENT","name":"UBS Current","initialBalance":{"amount":"1500.00","currency":"chf"}}""",
                // missing amount (@NotNull on the nested Money)
                """
                {"type":"CURRENT","name":"UBS Current","initialBalance":{"currency":"CHF"}}""",
                // missing currency (@NotNull on the nested Money)
                """
                {"type":"CURRENT","name":"UBS Current","initialBalance":{"amount":"1500.00"}}""");
    }

    @ParameterizedTest
    @MethodSource("invalidPayloads")
    @DisplayName("POST rejects each DTO constraint violation with 400 before reaching the use case")
    void openAccountRejectsInvalidPayloads(String body) throws Exception {
        mockMvc.perform(post("/api/accounts")
                        .with(oidcLogin())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));

        verifyNoInteractions(openAccount);
    }

    @Test
    @DisplayName("POST with an invalid IBAN surfaces the domain exception as 400")
    void openAccountRejectsInvalidIban() throws Exception {
        when(currentUserProvider.currentUserId()).thenReturn(OWNER);

        String body = """
                {"type":"CURRENT","name":"UBS Current","iban":"CH0000000000000000000",
                 "initialBalance":{"amount":"1500.00","currency":"CHF"}}""";

        mockMvc.perform(post("/api/accounts")
                        .with(oidcLogin())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));

        verifyNoInteractions(openAccount);
    }

    @Test
    @DisplayName("GET returns the owner's accounts, including a credit card without an IBAN")
    void listAccountsReturnsOk() throws Exception {
        when(currentUserProvider.currentUserId()).thenReturn(OWNER);
        when(listAccounts.list(any())).thenReturn(List.of(currentAccountView(), creditCardView()));

        mockMvc.perform(get("/api/accounts").with(oidcLogin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id").value(ACCOUNT_ID.toString()))
                .andExpect(jsonPath("$[0].status").value("ACTIVE"))
                .andExpect(jsonPath("$[0].iban").value(IBAN))
                .andExpect(jsonPath("$[1].type").value("CREDIT_CARD"))
                .andExpect(jsonPath("$[1].name").value("UBS Visa"));
    }

    @Test
    @DisplayName("GET returns 200 with an empty array when the owner has no account")
    void listAccountsReturnsEmpty() throws Exception {
        when(currentUserProvider.currentUserId()).thenReturn(OWNER);
        when(listAccounts.list(any())).thenReturn(List.of());

        mockMvc.perform(get("/api/accounts").with(oidcLogin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @DisplayName("DELETE archives the account and returns 204")
    void archiveAccountReturnsNoContent() throws Exception {
        when(currentUserProvider.currentUserId()).thenReturn(OWNER);

        mockMvc.perform(delete("/api/accounts/{accountId}", ACCOUNT_ID)
                        .with(oidcLogin())
                        .with(csrf()))
                .andExpect(status().isNoContent());

        ArgumentCaptor<ArchiveAccountCommand> captor = ArgumentCaptor.forClass(ArchiveAccountCommand.class);
        verify(archiveAccount).archive(captor.capture());
        assertThat(captor.getValue().owner()).isEqualTo(OWNER);
        assertThat(captor.getValue().accountId()).isEqualTo(AccountId.of(ACCOUNT_ID));
    }

    @Test
    @DisplayName("DELETE on an unknown (or foreign) account returns 404")
    void archiveAccountReturnsNotFound() throws Exception {
        when(currentUserProvider.currentUserId()).thenReturn(OWNER);
        doThrow(new AccountNotFoundException(AccountId.of(ACCOUNT_ID)))
                .when(archiveAccount)
                .archive(any());

        mockMvc.perform(delete("/api/accounts/{accountId}", ACCOUNT_ID)
                        .with(oidcLogin())
                        .with(csrf()))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @DisplayName("DELETE on an already-archived account returns 409 (not 400, despite being a DomainException)")
    void archiveAccountReturnsConflict() throws Exception {
        when(currentUserProvider.currentUserId()).thenReturn(OWNER);
        doThrow(new AccountAlreadyArchivedException(AccountId.of(ACCOUNT_ID)))
                .when(archiveAccount)
                .archive(any());

        mockMvc.perform(delete("/api/accounts/{accountId}", ACCOUNT_ID)
                        .with(oidcLogin())
                        .with(csrf()))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(409));
    }
}
