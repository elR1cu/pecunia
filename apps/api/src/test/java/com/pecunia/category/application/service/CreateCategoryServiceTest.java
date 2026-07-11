package com.pecunia.category.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pecunia.category.application.exception.CategoryTypeMismatchException;
import com.pecunia.category.application.exception.InvalidParentCategoryException;
import com.pecunia.category.application.port.in.CreateCategoryCommand;
import com.pecunia.category.application.port.out.CategoryRepository;
import com.pecunia.category.domain.Category;
import com.pecunia.category.domain.CategoryType;
import com.pecunia.category.domain.HexColor;
import com.pecunia.sharedkernel.CategoryId;
import com.pecunia.sharedkernel.IdGenerator;
import com.pecunia.sharedkernel.UserId;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CreateCategoryServiceTest {

    private static final UserId OWNER = UserId.of(UUID.randomUUID());
    private static final UUID NEW_ID = UUID.randomUUID();
    private static final CategoryId PARENT_ID = CategoryId.of(UUID.randomUUID());
    private static final HexColor COLOR = new HexColor("#33AA55");

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private IdGenerator idGenerator;

    private CreateCategoryService service;

    @BeforeEach
    void setUp() {
        // real validator on the mocked repository: the tests assert validation
        // behavior, not interactions with the helper
        service = new CreateCategoryService(
                categoryRepository, new ParentCategoryValidator(categoryRepository), idGenerator);
    }

    private static CreateCategoryCommand command(Optional<CategoryId> parent) {
        return new CreateCategoryCommand(
                OWNER, "Groceries", CategoryType.EXPENSE, COLOR, Optional.of("shopping_cart"), parent, 0);
    }

    private static Category parentCategory(CategoryType type) {
        return Category.create(PARENT_ID, OWNER, type, "Household", COLOR, null, null, 0);
    }

    @Test
    @DisplayName("creates a root category with a generated id and persists it")
    void creates_root_category() {
        // given
        CreateCategoryCommand command = command(Optional.empty());
        when(idGenerator.newId()).thenReturn(NEW_ID);

        // when
        CategoryId returnedId = service.create(command);

        // then
        assertThat(returnedId).isEqualTo(CategoryId.of(NEW_ID));
        ArgumentCaptor<Category> saved = ArgumentCaptor.forClass(Category.class);
        verify(categoryRepository).save(saved.capture());
        Category category = saved.getValue();
        assertThat(category.id()).isEqualTo(CategoryId.of(NEW_ID));
        assertThat(category.owner()).isEqualTo(OWNER);
        assertThat(category.type()).isEqualTo(CategoryType.EXPENSE);
        assertThat(category.name()).isEqualTo("Groceries");
        assertThat(category.color()).isEqualTo(COLOR);
        assertThat(category.icon()).contains("shopping_cart");
        assertThat(category.parent()).isEmpty();
        assertThat(category.displayOrder()).isZero();
        assertThat(category.archived()).isFalse();
    }

    @Test
    @DisplayName("creates a child category under a valid parent of the same type")
    void creates_child_under_valid_parent() {
        // given
        CreateCategoryCommand command = command(Optional.of(PARENT_ID));
        when(idGenerator.newId()).thenReturn(NEW_ID);
        when(categoryRepository.findByIdAndOwner(PARENT_ID, OWNER))
                .thenReturn(Optional.of(parentCategory(CategoryType.EXPENSE)));

        // when
        service.create(command);

        // then
        ArgumentCaptor<Category> saved = ArgumentCaptor.forClass(Category.class);
        verify(categoryRepository).save(saved.capture());
        assertThat(saved.getValue().parent()).contains(PARENT_ID);
    }

    @Test
    @DisplayName("throws InvalidParentCategoryException when the parent is absent or not owned")
    void rejects_missing_parent() {
        // given
        CreateCategoryCommand command = command(Optional.of(PARENT_ID));
        when(categoryRepository.findByIdAndOwner(PARENT_ID, OWNER)).thenReturn(Optional.empty());

        // when + then
        assertThatThrownBy(() -> service.create(command)).isInstanceOf(InvalidParentCategoryException.class);
        verify(categoryRepository, never()).save(any());
    }

    @Test
    @DisplayName("throws InvalidParentCategoryException when the parent is archived")
    void rejects_archived_parent() {
        // given
        CreateCategoryCommand command = command(Optional.of(PARENT_ID));
        Category parent = parentCategory(CategoryType.EXPENSE);
        parent.archive();
        when(categoryRepository.findByIdAndOwner(PARENT_ID, OWNER)).thenReturn(Optional.of(parent));

        // when + then
        assertThatThrownBy(() -> service.create(command)).isInstanceOf(InvalidParentCategoryException.class);
        verify(categoryRepository, never()).save(any());
    }

    @Test
    @DisplayName("throws CategoryTypeMismatchException when the parent has a different type")
    void rejects_parent_type_mismatch() {
        // given
        CreateCategoryCommand command = command(Optional.of(PARENT_ID));
        when(categoryRepository.findByIdAndOwner(PARENT_ID, OWNER))
                .thenReturn(Optional.of(parentCategory(CategoryType.INCOME)));

        // when + then
        assertThatThrownBy(() -> service.create(command)).isInstanceOf(CategoryTypeMismatchException.class);
        verify(categoryRepository, never()).save(any());
    }
}
