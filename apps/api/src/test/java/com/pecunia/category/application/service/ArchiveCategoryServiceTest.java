package com.pecunia.category.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pecunia.category.application.exception.CategoryNotFoundException;
import com.pecunia.category.application.port.in.ArchiveCategoryCommand;
import com.pecunia.category.application.port.out.CategoryRepository;
import com.pecunia.category.domain.Category;
import com.pecunia.category.domain.CategoryType;
import com.pecunia.category.domain.HexColor;
import com.pecunia.category.domain.exception.CategoryAlreadyArchivedException;
import com.pecunia.sharedkernel.CategoryId;
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
class ArchiveCategoryServiceTest {

    private static final UserId OWNER = UserId.of(UUID.randomUUID());
    private static final CategoryId CATEGORY_ID = CategoryId.of(UUID.randomUUID());
    private static final HexColor COLOR = new HexColor("#33AA55");

    @Mock
    private CategoryRepository categoryRepository;

    private ArchiveCategoryService service;

    @BeforeEach
    void setUp() {
        service = new ArchiveCategoryService(categoryRepository);
    }

    private static Category activeCategory() {
        return Category.create(CATEGORY_ID, OWNER, CategoryType.EXPENSE, "Groceries", COLOR, null, null, 0);
    }

    @Test
    @DisplayName("archives an active category and persists it")
    void archives_active_category() {
        // given
        ArchiveCategoryCommand command = new ArchiveCategoryCommand(OWNER, CATEGORY_ID);
        Category category = activeCategory();
        when(categoryRepository.findByIdAndOwner(CATEGORY_ID, OWNER)).thenReturn(Optional.of(category));

        // when
        service.archive(command);

        // then
        assertThat(category.archived()).isTrue();
        verify(categoryRepository).save(category);
    }

    @Test
    @DisplayName("throws CategoryNotFoundException when the category is absent or not owned")
    void rejects_missing_category() {
        // given
        ArchiveCategoryCommand command = new ArchiveCategoryCommand(OWNER, CATEGORY_ID);
        when(categoryRepository.findByIdAndOwner(CATEGORY_ID, OWNER)).thenReturn(Optional.empty());

        // when + then
        assertThatThrownBy(() -> service.archive(command)).isInstanceOf(CategoryNotFoundException.class);
        verify(categoryRepository, never()).save(any());
    }

    @Test
    @DisplayName("propagates CategoryAlreadyArchivedException and does not persist")
    void rejects_double_archive() {
        // given
        ArchiveCategoryCommand command = new ArchiveCategoryCommand(OWNER, CATEGORY_ID);
        Category category = activeCategory();
        category.archive(); // already archived
        when(categoryRepository.findByIdAndOwner(CATEGORY_ID, OWNER)).thenReturn(Optional.of(category));

        // when + then
        assertThatThrownBy(() -> service.archive(command)).isInstanceOf(CategoryAlreadyArchivedException.class);
        verify(categoryRepository, never()).save(any());
    }
}
