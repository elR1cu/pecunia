package com.pecunia.category.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pecunia.category.application.exception.CategoryCycleException;
import com.pecunia.category.application.exception.CategoryNotFoundException;
import com.pecunia.category.application.exception.CategoryTypeMismatchException;
import com.pecunia.category.application.exception.InvalidParentCategoryException;
import com.pecunia.category.application.port.in.MoveCategoryToParentCommand;
import com.pecunia.category.application.port.out.CategoryRepository;
import com.pecunia.category.domain.Category;
import com.pecunia.category.domain.CategoryType;
import com.pecunia.category.domain.HexColor;
import com.pecunia.category.domain.exception.ArchivedCategoryModificationException;
import com.pecunia.sharedkernel.CategoryId;
import com.pecunia.sharedkernel.UserId;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MoveCategoryToParentServiceTest {

    private static final UserId OWNER = UserId.of(UUID.randomUUID());
    private static final CategoryId CATEGORY_ID = CategoryId.of(UUID.randomUUID());
    private static final CategoryId PARENT_ID = CategoryId.of(UUID.randomUUID());
    private static final CategoryId OLD_PARENT_ID = CategoryId.of(UUID.randomUUID());
    private static final HexColor COLOR = new HexColor("#33AA55");

    @Mock
    private CategoryRepository categoryRepository;

    private MoveCategoryToParentService service;

    @BeforeEach
    void setUp() {
        // real validator on the mocked repository: the tests assert validation
        // behavior, not interactions with the helper
        service = new MoveCategoryToParentService(categoryRepository, new ParentCategoryValidator(categoryRepository));
    }

    private static MoveCategoryToParentCommand moveCommand(Optional<CategoryId> newParent) {
        return new MoveCategoryToParentCommand(OWNER, CATEGORY_ID, newParent);
    }

    private static Category category(CategoryId id, CategoryType type, CategoryId parent) {
        return Category.create(id, OWNER, type, "Groceries", COLOR, null, parent, 0);
    }

    @Test
    @DisplayName("moves the category under a valid parent of the same type")
    void moves_under_valid_parent() {
        // given
        MoveCategoryToParentCommand command = moveCommand(Optional.of(PARENT_ID));
        Category moved = category(CATEGORY_ID, CategoryType.EXPENSE, null);
        Category parent = category(PARENT_ID, CategoryType.EXPENSE, null);
        when(categoryRepository.findByIdAndOwner(CATEGORY_ID, OWNER)).thenReturn(Optional.of(moved));
        when(categoryRepository.findByIdAndOwner(PARENT_ID, OWNER)).thenReturn(Optional.of(parent));
        when(categoryRepository.findAncestorIds(PARENT_ID, OWNER)).thenReturn(Set.of());

        // when
        service.moveToParent(command);

        // then
        assertThat(moved.parent()).contains(PARENT_ID);
        verify(categoryRepository).save(moved);
    }

    @Test
    @DisplayName("detaches the category to the root when the command carries no parent")
    void detaches_to_root() {
        // given
        MoveCategoryToParentCommand command = moveCommand(Optional.empty());
        Category moved = category(CATEGORY_ID, CategoryType.EXPENSE, OLD_PARENT_ID);
        when(categoryRepository.findByIdAndOwner(CATEGORY_ID, OWNER)).thenReturn(Optional.of(moved));

        // when
        service.moveToParent(command);

        // then
        assertThat(moved.parent()).isEmpty();
        verify(categoryRepository).save(moved);
    }

    @Test
    @DisplayName("throws CategoryNotFoundException when the category is absent or not owned")
    void rejects_missing_category() {
        // given
        MoveCategoryToParentCommand command = moveCommand(Optional.of(PARENT_ID));
        when(categoryRepository.findByIdAndOwner(CATEGORY_ID, OWNER)).thenReturn(Optional.empty());

        // when + then
        assertThatThrownBy(() -> service.moveToParent(command)).isInstanceOf(CategoryNotFoundException.class);
        verify(categoryRepository, never()).save(any());
    }

    @Test
    @DisplayName("throws ArchivedCategoryModificationException when the category is archived")
    void rejects_archived_category() {
        // given
        MoveCategoryToParentCommand command = moveCommand(Optional.of(PARENT_ID));
        Category moved = category(CATEGORY_ID, CategoryType.EXPENSE, null);
        moved.archive();
        when(categoryRepository.findByIdAndOwner(CATEGORY_ID, OWNER)).thenReturn(Optional.of(moved));

        // when + then
        assertThatThrownBy(() -> service.moveToParent(command))
                .isInstanceOf(ArchivedCategoryModificationException.class);
        verify(categoryRepository, never()).save(any());
    }

    @Test
    @DisplayName("throws InvalidParentCategoryException when the parent is absent or not owned")
    void rejects_missing_parent() {
        // given
        MoveCategoryToParentCommand command = moveCommand(Optional.of(PARENT_ID));
        Category moved = category(CATEGORY_ID, CategoryType.EXPENSE, null);
        when(categoryRepository.findByIdAndOwner(CATEGORY_ID, OWNER)).thenReturn(Optional.of(moved));
        when(categoryRepository.findByIdAndOwner(PARENT_ID, OWNER)).thenReturn(Optional.empty());

        // when + then
        assertThatThrownBy(() -> service.moveToParent(command)).isInstanceOf(InvalidParentCategoryException.class);
        verify(categoryRepository, never()).save(any());
    }

    @Test
    @DisplayName("throws InvalidParentCategoryException when the parent is archived")
    void rejects_archived_parent() {
        // given
        MoveCategoryToParentCommand command = moveCommand(Optional.of(PARENT_ID));
        Category moved = category(CATEGORY_ID, CategoryType.EXPENSE, null);
        Category parent = category(PARENT_ID, CategoryType.EXPENSE, null);
        parent.archive();
        when(categoryRepository.findByIdAndOwner(CATEGORY_ID, OWNER)).thenReturn(Optional.of(moved));
        when(categoryRepository.findByIdAndOwner(PARENT_ID, OWNER)).thenReturn(Optional.of(parent));

        // when + then
        assertThatThrownBy(() -> service.moveToParent(command)).isInstanceOf(InvalidParentCategoryException.class);
        verify(categoryRepository, never()).save(any());
    }

    @Test
    @DisplayName("throws CategoryTypeMismatchException when the parent has a different type")
    void rejects_parent_type_mismatch() {
        // given
        MoveCategoryToParentCommand command = moveCommand(Optional.of(PARENT_ID));
        Category moved = category(CATEGORY_ID, CategoryType.EXPENSE, null);
        Category parent = category(PARENT_ID, CategoryType.INCOME, null);
        when(categoryRepository.findByIdAndOwner(CATEGORY_ID, OWNER)).thenReturn(Optional.of(moved));
        when(categoryRepository.findByIdAndOwner(PARENT_ID, OWNER)).thenReturn(Optional.of(parent));

        // when + then
        assertThatThrownBy(() -> service.moveToParent(command)).isInstanceOf(CategoryTypeMismatchException.class);
        verify(categoryRepository, never()).save(any());
    }

    @Test
    @DisplayName("throws CategoryCycleException when the category is moved under itself")
    void rejects_self_parent() {
        // given
        MoveCategoryToParentCommand command = moveCommand(Optional.of(CATEGORY_ID));
        Category moved = category(CATEGORY_ID, CategoryType.EXPENSE, null);
        when(categoryRepository.findByIdAndOwner(CATEGORY_ID, OWNER)).thenReturn(Optional.of(moved));

        // when + then
        assertThatThrownBy(() -> service.moveToParent(command)).isInstanceOf(CategoryCycleException.class);
        verify(categoryRepository, never()).save(any());
    }

    @Test
    @DisplayName("throws CategoryCycleException when the category is moved under one of its descendants")
    void rejects_descendant_parent() {
        // given
        MoveCategoryToParentCommand command = moveCommand(Optional.of(PARENT_ID));
        Category moved = category(CATEGORY_ID, CategoryType.EXPENSE, null);
        Category parent = category(PARENT_ID, CategoryType.EXPENSE, CATEGORY_ID);
        when(categoryRepository.findByIdAndOwner(CATEGORY_ID, OWNER)).thenReturn(Optional.of(moved));
        when(categoryRepository.findByIdAndOwner(PARENT_ID, OWNER)).thenReturn(Optional.of(parent));
        when(categoryRepository.findAncestorIds(PARENT_ID, OWNER)).thenReturn(Set.of(CATEGORY_ID));

        // when + then
        assertThatThrownBy(() -> service.moveToParent(command)).isInstanceOf(CategoryCycleException.class);
        verify(categoryRepository, never()).save(any());
    }
}
