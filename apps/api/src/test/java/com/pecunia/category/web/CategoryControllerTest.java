package com.pecunia.category.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.pecunia.category.application.exception.CategoryCycleException;
import com.pecunia.category.application.exception.CategoryNotFoundException;
import com.pecunia.category.application.exception.CategoryTypeMismatchException;
import com.pecunia.category.application.exception.InvalidParentCategoryException;
import com.pecunia.category.application.port.in.ArchiveCategory;
import com.pecunia.category.application.port.in.ArchiveCategoryCommand;
import com.pecunia.category.application.port.in.CreateCategory;
import com.pecunia.category.application.port.in.CreateCategoryCommand;
import com.pecunia.category.application.port.in.GetCategory;
import com.pecunia.category.application.port.in.GetCategoryQuery;
import com.pecunia.category.application.port.in.ListCategories;
import com.pecunia.category.application.port.in.MoveCategoryToParent;
import com.pecunia.category.application.port.in.MoveCategoryToParentCommand;
import com.pecunia.category.application.port.in.RecolorCategory;
import com.pecunia.category.application.port.in.RecolorCategoryCommand;
import com.pecunia.category.application.port.in.ReiconCategory;
import com.pecunia.category.application.port.in.ReiconCategoryCommand;
import com.pecunia.category.application.port.in.RenameCategory;
import com.pecunia.category.application.port.in.RenameCategoryCommand;
import com.pecunia.category.application.port.in.ReorderCategory;
import com.pecunia.category.application.port.in.ReorderCategoryCommand;
import com.pecunia.category.application.readmodel.CategoryNode;
import com.pecunia.category.application.readmodel.CategoryView;
import com.pecunia.category.domain.CategoryType;
import com.pecunia.category.domain.HexColor;
import com.pecunia.category.domain.exception.ArchivedCategoryModificationException;
import com.pecunia.category.domain.exception.CategoryAlreadyArchivedException;
import com.pecunia.category.web.mapper.CategoryMapperImpl;
import com.pecunia.sharedinfra.security.PecuniaOidcUserService;
import com.pecunia.sharedinfra.security.SecurityConfig;
import com.pecunia.sharedkernel.CategoryId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Web-slice tests for {@link CategoryController}. The real {@link CategoryMapperImpl} is imported
 * (not mocked) so the DTO/command mapping is exercised end to end; the driving ports are mocked.
 * The {@code category}-scoped {@code @RestControllerAdvice} is auto-detected by {@code @WebMvcTest},
 * so the 404/409/422 mappings of ADR-0034 are covered here too.
 */
@WebMvcTest(CategoryController.class)
@Import({SecurityConfig.class, CategoryMapperImpl.class})
class CategoryControllerTest {

    private static final UUID CATEGORY_ID = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000001");
    private static final UUID PARENT_ID = UUID.fromString("bbbbbbbb-0000-0000-0000-000000000002");
    private static final UUID CHILD_ID = UUID.fromString("cccccccc-0000-0000-0000-000000000003");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CreateCategory createCategory;

    @MockitoBean
    private GetCategory getCategory;

    @MockitoBean
    private ListCategories listCategories;

    @MockitoBean
    private MoveCategoryToParent moveCategoryToParent;

    @MockitoBean
    private RenameCategory renameCategory;

    @MockitoBean
    private RecolorCategory recolorCategory;

    @MockitoBean
    private ReiconCategory reiconCategory;

    @MockitoBean
    private ReorderCategory reorderCategory;

    @MockitoBean
    private ArchiveCategory archiveCategory;

    /** Keeps the slice hermetic: prevents a real OIDC discovery call to Keycloak at context load. */
    @MockitoBean
    private ClientRegistrationRepository clientRegistrationRepository;

    /** Referenced by SecurityConfig's OAuth2 login wiring; not exercised by this slice. */
    @MockitoBean
    private PecuniaOidcUserService pecuniaOidcUserService;

    private static CategoryView childView() {
        return new CategoryView(
                CategoryId.of(CATEGORY_ID),
                "Groceries",
                CategoryType.EXPENSE,
                new HexColor("#1A2B3C"),
                Optional.of("shopping_cart"),
                2,
                false,
                Optional.of(CategoryId.of(PARENT_ID)));
    }

    private static CategoryNode treeRoot() {
        CategoryNode child = new CategoryNode(
                CategoryId.of(CHILD_ID),
                "Groceries",
                CategoryType.EXPENSE,
                new HexColor("#1A2B3C"),
                Optional.of("shopping_cart"),
                0,
                false,
                List.of());
        return new CategoryNode(
                CategoryId.of(CATEGORY_ID),
                "Food",
                CategoryType.EXPENSE,
                new HexColor("#AABBCC"),
                Optional.empty(),
                0,
                false,
                List.of(child));
    }

    // --- createCategory ---------------------------------------------------------

    @Test
    @DisplayName("POST creates a category and returns 201 with a Location header and the created body")
    void createCategoryReturnsCreated() throws Exception {
        when(createCategory.create(any())).thenReturn(CategoryId.of(CATEGORY_ID));
        when(getCategory.getById(new GetCategoryQuery(CategoryId.of(CATEGORY_ID))))
                .thenReturn(childView());

        String body = """
                {"type":"EXPENSE","name":"Groceries","color":"#1A2B3C","icon":"shopping_cart",
                 "parentId":"bbbbbbbb-0000-0000-0000-000000000002","displayOrder":2}""";

        mockMvc.perform(post("/api/categories")
                        .with(oidcLogin())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", endsWith("/api/categories/" + CATEGORY_ID)))
                .andExpect(jsonPath("$.id").value(CATEGORY_ID.toString()))
                .andExpect(jsonPath("$.type").value("EXPENSE"))
                .andExpect(jsonPath("$.name").value("Groceries"))
                .andExpect(jsonPath("$.color").value("#1A2B3C"))
                .andExpect(jsonPath("$.icon").value("shopping_cart"))
                .andExpect(jsonPath("$.parentId").value(PARENT_ID.toString()))
                .andExpect(jsonPath("$.displayOrder").value(2))
                .andExpect(jsonPath("$.archived").value(false));

        ArgumentCaptor<CreateCategoryCommand> captor = ArgumentCaptor.forClass(CreateCategoryCommand.class);
        verify(createCategory).create(captor.capture());
        CreateCategoryCommand command = captor.getValue();
        assertThat(command.name()).isEqualTo("Groceries");
        assertThat(command.type()).isEqualTo(CategoryType.EXPENSE);
        assertThat(command.color()).isEqualTo(new HexColor("#1A2B3C"));
        assertThat(command.icon()).contains("shopping_cart");
        assertThat(command.parent()).contains(CategoryId.of(PARENT_ID));
        assertThat(command.displayOrder()).isEqualTo(2);
    }

    private static Stream<String> invalidCreatePayloads() {
        String tooLongName = "A".repeat(101);
        String tooLongIcon = "i".repeat(51);
        return Stream.of(
                // missing type
                """
                {"name":"Groceries","color":"#1A2B3C","displayOrder":0}""",
                // blank name (minLength 1)
                """
                {"type":"EXPENSE","name":"","color":"#1A2B3C","displayOrder":0}""",
                // name longer than 100 chars
                "{\"type\":\"EXPENSE\",\"name\":\"" + tooLongName + "\",\"color\":\"#1A2B3C\",\"displayOrder\":0}",
                // missing color
                """
                {"type":"EXPENSE","name":"Groceries","displayOrder":0}""",
                // color not matching the #RRGGBB pattern
                """
                {"type":"EXPENSE","name":"Groceries","color":"blue","displayOrder":0}""",
                // icon longer than 50 chars
                "{\"type\":\"EXPENSE\",\"name\":\"Groceries\",\"color\":\"#1A2B3C\",\"icon\":\"" + tooLongIcon
                        + "\",\"displayOrder\":0}",
                // negative displayOrder (minimum 0)
                """
                {"type":"EXPENSE","name":"Groceries","color":"#1A2B3C","displayOrder":-1}""",
                // missing displayOrder
                """
                {"type":"EXPENSE","name":"Groceries","color":"#1A2B3C"}""");
    }

    @ParameterizedTest
    @MethodSource("invalidCreatePayloads")
    @DisplayName("POST rejects each DTO constraint violation with 400 before reaching the use case")
    void createCategoryRejectsInvalidPayloads(String body) throws Exception {
        mockMvc.perform(post("/api/categories")
                        .with(oidcLogin())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));

        verifyNoInteractions(createCategory);
    }

    @Test
    @DisplayName("POST with an invalid parent returns 422 (well-formed but unprocessable)")
    void createCategoryRejectsInvalidParent() throws Exception {
        doThrow(new InvalidParentCategoryException(CategoryId.of(PARENT_ID)))
                .when(createCategory)
                .create(any());

        String body = """
                {"type":"EXPENSE","name":"Groceries","color":"#1A2B3C",
                 "parentId":"bbbbbbbb-0000-0000-0000-000000000002","displayOrder":0}""";

        mockMvc.perform(post("/api/categories")
                        .with(oidcLogin())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.status").value(422));
    }

    @Test
    @DisplayName("POST with a parent of a different type returns 422")
    void createCategoryRejectsTypeMismatch() throws Exception {
        doThrow(new CategoryTypeMismatchException(CategoryType.EXPENSE, CategoryType.INCOME))
                .when(createCategory)
                .create(any());

        String body = """
                {"type":"EXPENSE","name":"Groceries","color":"#1A2B3C",
                 "parentId":"bbbbbbbb-0000-0000-0000-000000000002","displayOrder":0}""";

        mockMvc.perform(post("/api/categories")
                        .with(oidcLogin())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.status").value(422));
    }

    // --- getCategory ------------------------------------------------------------

    @Test
    @DisplayName("GET returns the category")
    void getCategoryReturnsOk() throws Exception {
        when(getCategory.getById(new GetCategoryQuery(CategoryId.of(CATEGORY_ID))))
                .thenReturn(childView());

        mockMvc.perform(get("/api/categories/{categoryId}", CATEGORY_ID).with(oidcLogin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(CATEGORY_ID.toString()))
                .andExpect(jsonPath("$.name").value("Groceries"))
                .andExpect(jsonPath("$.parentId").value(PARENT_ID.toString()));
    }

    @Test
    @DisplayName("GET on an unknown (or foreign) category returns 404")
    void getCategoryReturnsNotFound() throws Exception {
        when(getCategory.getById(any())).thenThrow(new CategoryNotFoundException(CategoryId.of(CATEGORY_ID)));

        mockMvc.perform(get("/api/categories/{categoryId}", CATEGORY_ID).with(oidcLogin()))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(404));
    }

    // --- listCategories ---------------------------------------------------------

    @Test
    @DisplayName("GET returns the category tree with nested children")
    void listCategoriesReturnsTree() throws Exception {
        when(listCategories.list()).thenReturn(List.of(treeRoot()));

        mockMvc.perform(get("/api/categories").with(oidcLogin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(CATEGORY_ID.toString()))
                .andExpect(jsonPath("$[0].name").value("Food"))
                .andExpect(jsonPath("$[0].icon").value(nullValue()))
                .andExpect(jsonPath("$[0].children", hasSize(1)))
                .andExpect(jsonPath("$[0].children[0].id").value(CHILD_ID.toString()))
                .andExpect(jsonPath("$[0].children[0].name").value("Groceries"))
                .andExpect(jsonPath("$[0].children[0].icon").value("shopping_cart"));
    }

    @Test
    @DisplayName("GET returns 200 with an empty array when the owner has no category")
    void listCategoriesReturnsEmpty() throws Exception {
        when(listCategories.list()).thenReturn(List.of());

        mockMvc.perform(get("/api/categories").with(oidcLogin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    // --- updateCategory ---------------------------------------------------------

    @Test
    @DisplayName("PATCH dispatches one command per present field and returns 200 with the fresh body")
    void updateCategoryDispatchesEachField() throws Exception {
        when(getCategory.getById(new GetCategoryQuery(CategoryId.of(CATEGORY_ID))))
                .thenReturn(childView());

        String body = """
                {"name":"Restaurants","color":"#FFAA00","icon":"restaurant","displayOrder":5}""";

        mockMvc.perform(patch("/api/categories/{categoryId}", CATEGORY_ID)
                        .with(oidcLogin())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(CATEGORY_ID.toString()));

        ArgumentCaptor<RenameCategoryCommand> rename = ArgumentCaptor.forClass(RenameCategoryCommand.class);
        verify(renameCategory).rename(rename.capture());
        assertThat(rename.getValue().newName()).isEqualTo("Restaurants");

        ArgumentCaptor<RecolorCategoryCommand> recolor = ArgumentCaptor.forClass(RecolorCategoryCommand.class);
        verify(recolorCategory).recolor(recolor.capture());
        assertThat(recolor.getValue().newColor()).isEqualTo(new HexColor("#FFAA00"));

        ArgumentCaptor<ReiconCategoryCommand> reicon = ArgumentCaptor.forClass(ReiconCategoryCommand.class);
        verify(reiconCategory).reicon(reicon.capture());
        assertThat(reicon.getValue().newIcon()).contains("restaurant");

        ArgumentCaptor<ReorderCategoryCommand> reorder = ArgumentCaptor.forClass(ReorderCategoryCommand.class);
        verify(reorderCategory).reorder(reorder.capture());
        assertThat(reorder.getValue().newDisplayOrder()).isEqualTo(5);
    }

    @Test
    @DisplayName("PATCH with only a name touches rename alone")
    void updateCategoryDispatchesOnlyPresentField() throws Exception {
        when(getCategory.getById(new GetCategoryQuery(CategoryId.of(CATEGORY_ID))))
                .thenReturn(childView());

        mockMvc.perform(patch("/api/categories/{categoryId}", CATEGORY_ID)
                        .with(oidcLogin())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Restaurants\"}"))
                .andExpect(status().isOk());

        verify(renameCategory).rename(any());
        verifyNoInteractions(recolorCategory, reiconCategory, reorderCategory);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    @DisplayName("PATCH with a blank icon clears it (Optional.empty)")
    void updateCategoryClearsIcon(String blankIcon) throws Exception {
        when(getCategory.getById(new GetCategoryQuery(CategoryId.of(CATEGORY_ID))))
                .thenReturn(childView());

        mockMvc.perform(patch("/api/categories/{categoryId}", CATEGORY_ID)
                        .with(oidcLogin())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"icon\":\"" + blankIcon + "\"}"))
                .andExpect(status().isOk());

        ArgumentCaptor<ReiconCategoryCommand> reicon = ArgumentCaptor.forClass(ReiconCategoryCommand.class);
        verify(reiconCategory).reicon(reicon.capture());
        assertThat(reicon.getValue().newIcon()).isEmpty();
    }

    @Test
    @DisplayName("PATCH with an empty body dispatches nothing and returns the current category")
    void updateCategoryEmptyPatchIsNoOp() throws Exception {
        when(getCategory.getById(new GetCategoryQuery(CategoryId.of(CATEGORY_ID))))
                .thenReturn(childView());

        mockMvc.perform(patch("/api/categories/{categoryId}", CATEGORY_ID)
                        .with(oidcLogin())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(CATEGORY_ID.toString()));

        verifyNoInteractions(renameCategory, recolorCategory, reiconCategory, reorderCategory);
    }

    @Test
    @DisplayName("PATCH on an archived category returns 409")
    void updateCategoryOnArchivedReturnsConflict() throws Exception {
        doThrow(new ArchivedCategoryModificationException(CategoryId.of(CATEGORY_ID)))
                .when(renameCategory)
                .rename(any());

        mockMvc.perform(patch("/api/categories/{categoryId}", CATEGORY_ID)
                        .with(oidcLogin())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Restaurants\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    @DisplayName("PATCH on an unknown category returns 404")
    void updateCategoryOnUnknownReturnsNotFound() throws Exception {
        doThrow(new CategoryNotFoundException(CategoryId.of(CATEGORY_ID)))
                .when(renameCategory)
                .rename(any());

        mockMvc.perform(patch("/api/categories/{categoryId}", CATEGORY_ID)
                        .with(oidcLogin())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Restaurants\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @DisplayName("PATCH with a malformed color is rejected with 400 before any dispatch")
    void updateCategoryRejectsInvalidPayload() throws Exception {
        mockMvc.perform(patch("/api/categories/{categoryId}", CATEGORY_ID)
                        .with(oidcLogin())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"color\":\"not-a-color\"}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(recolorCategory);
    }

    // --- moveCategory -----------------------------------------------------------

    @Test
    @DisplayName("POST /move re-parents the category and returns 204")
    void moveCategoryReturnsNoContent() throws Exception {
        mockMvc.perform(post("/api/categories/{categoryId}/move", CATEGORY_ID)
                        .with(oidcLogin())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"newParentId\":\"bbbbbbbb-0000-0000-0000-000000000002\"}"))
                .andExpect(status().isNoContent());

        ArgumentCaptor<MoveCategoryToParentCommand> captor = ArgumentCaptor.forClass(MoveCategoryToParentCommand.class);
        verify(moveCategoryToParent).moveToParent(captor.capture());
        assertThat(captor.getValue().categoryId()).isEqualTo(CategoryId.of(CATEGORY_ID));
        assertThat(captor.getValue().newParent()).contains(CategoryId.of(PARENT_ID));
    }

    @Test
    @DisplayName("POST /move with a null newParentId detaches the category to a root")
    void moveCategoryDetaches() throws Exception {
        mockMvc.perform(post("/api/categories/{categoryId}/move", CATEGORY_ID)
                        .with(oidcLogin())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"newParentId\":null}"))
                .andExpect(status().isNoContent());

        ArgumentCaptor<MoveCategoryToParentCommand> captor = ArgumentCaptor.forClass(MoveCategoryToParentCommand.class);
        verify(moveCategoryToParent).moveToParent(captor.capture());
        assertThat(captor.getValue().newParent()).isEmpty();
    }

    @Test
    @DisplayName("POST /move that would create a cycle returns 409")
    void moveCategoryCycleReturnsConflict() throws Exception {
        doThrow(new CategoryCycleException(CategoryId.of(CATEGORY_ID), CategoryId.of(PARENT_ID)))
                .when(moveCategoryToParent)
                .moveToParent(any());

        mockMvc.perform(post("/api/categories/{categoryId}/move", CATEGORY_ID)
                        .with(oidcLogin())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"newParentId\":\"bbbbbbbb-0000-0000-0000-000000000002\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    @DisplayName("POST /move onto an invalid parent returns 422")
    void moveCategoryInvalidParentReturnsUnprocessable() throws Exception {
        doThrow(new InvalidParentCategoryException(CategoryId.of(PARENT_ID)))
                .when(moveCategoryToParent)
                .moveToParent(any());

        mockMvc.perform(post("/api/categories/{categoryId}/move", CATEGORY_ID)
                        .with(oidcLogin())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"newParentId\":\"bbbbbbbb-0000-0000-0000-000000000002\"}"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.status").value(422));
    }

    // --- archiveCategory --------------------------------------------------------

    @Test
    @DisplayName("DELETE archives the category and returns 204")
    void archiveCategoryReturnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/categories/{categoryId}", CATEGORY_ID)
                        .with(oidcLogin())
                        .with(csrf()))
                .andExpect(status().isNoContent());

        ArgumentCaptor<ArchiveCategoryCommand> captor = ArgumentCaptor.forClass(ArchiveCategoryCommand.class);
        verify(archiveCategory).archive(captor.capture());
        assertThat(captor.getValue().categoryId()).isEqualTo(CategoryId.of(CATEGORY_ID));
    }

    @Test
    @DisplayName("DELETE on an unknown (or foreign) category returns 404")
    void archiveCategoryReturnsNotFound() throws Exception {
        doThrow(new CategoryNotFoundException(CategoryId.of(CATEGORY_ID)))
                .when(archiveCategory)
                .archive(any());

        mockMvc.perform(delete("/api/categories/{categoryId}", CATEGORY_ID)
                        .with(oidcLogin())
                        .with(csrf()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @DisplayName("DELETE on an already-archived category returns 409")
    void archiveCategoryReturnsConflict() throws Exception {
        doThrow(new CategoryAlreadyArchivedException(CategoryId.of(CATEGORY_ID)))
                .when(archiveCategory)
                .archive(any());

        mockMvc.perform(delete("/api/categories/{categoryId}", CATEGORY_ID)
                        .with(oidcLogin())
                        .with(csrf()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }
}
