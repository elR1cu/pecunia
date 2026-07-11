package com.pecunia.category.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pecunia.category.application.exception.CategoryNotFoundException;
import com.pecunia.category.application.port.in.ReiconCategoryCommand;
import com.pecunia.category.application.port.out.CategoryRepository;
import com.pecunia.category.domain.Category;
import com.pecunia.category.domain.CategoryType;
import com.pecunia.category.domain.HexColor;
import com.pecunia.category.domain.exception.ArchivedCategoryModificationException;
import com.pecunia.sharedkernel.CategoryId;
import com.pecunia.sharedkernel.CurrentUserProvider;
import com.pecunia.sharedkernel.UserId;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReiconCategoryServiceTest {

    private static final UserId OWNER = UserId.of(UUID.randomUUID());
    private static final CategoryId CATEGORY_ID = CategoryId.of(UUID.randomUUID());
    private static final HexColor COLOR = new HexColor("#33AA55");

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private CurrentUserProvider currentUserProvider;

    private ReiconCategoryService service;

    @BeforeEach
    void setUp() {
        service = new ReiconCategoryService(categoryRepository, currentUserProvider);
    }

    private static Category categoryWithIcon() {
        return Category.create(CATEGORY_ID, OWNER, CategoryType.EXPENSE, "Groceries", COLOR, "shopping_cart", null, 0);
    }

    @Test
    @DisplayName("replaces the icon and persists the category")
    void reicons_category() {
        // given
        ReiconCategoryCommand command = new ReiconCategoryCommand(CATEGORY_ID, Optional.of("home"));
        Category category = categoryWithIcon();
        when(currentUserProvider.currentUserId()).thenReturn(OWNER);
        when(categoryRepository.findByIdAndOwner(CATEGORY_ID, OWNER)).thenReturn(Optional.of(category));

        // when
        service.reicon(command);

        // then
        assertThat(category.icon()).contains("home");
        verify(categoryRepository).save(category);
    }

    @Test
    @DisplayName("clears the icon when the command carries no icon")
    void clears_icon() {
        // given
        ReiconCategoryCommand command = new ReiconCategoryCommand(CATEGORY_ID, Optional.empty());
        Category category = categoryWithIcon();
        when(currentUserProvider.currentUserId()).thenReturn(OWNER);
        when(categoryRepository.findByIdAndOwner(CATEGORY_ID, OWNER)).thenReturn(Optional.of(category));

        // when
        service.reicon(command);

        // then
        assertThat(category.icon()).isEmpty();
        verify(categoryRepository).save(category);
    }

    @Test
    @DisplayName("throws CategoryNotFoundException when the category is absent or not owned")
    void rejects_missing_category() {
        // given
        ReiconCategoryCommand command = new ReiconCategoryCommand(CATEGORY_ID, Optional.of("home"));
        when(currentUserProvider.currentUserId()).thenReturn(OWNER);
        when(categoryRepository.findByIdAndOwner(CATEGORY_ID, OWNER)).thenReturn(Optional.empty());

        // when + then
        assertThatThrownBy(() -> service.reicon(command)).isInstanceOf(CategoryNotFoundException.class);
        verify(categoryRepository, never()).save(any());
    }

    @Test
    @DisplayName("propagates ArchivedCategoryModificationException and does not persist")
    void rejects_archived_category() {
        // given
        ReiconCategoryCommand command = new ReiconCategoryCommand(CATEGORY_ID, Optional.of("home"));
        Category category = categoryWithIcon();
        category.archive();
        when(currentUserProvider.currentUserId()).thenReturn(OWNER);
        when(categoryRepository.findByIdAndOwner(CATEGORY_ID, OWNER)).thenReturn(Optional.of(category));

        // when + then
        assertThatThrownBy(() -> service.reicon(command)).isInstanceOf(ArchivedCategoryModificationException.class);
        verify(categoryRepository, never()).save(any());
    }
}
