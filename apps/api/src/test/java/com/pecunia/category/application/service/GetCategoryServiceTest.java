package com.pecunia.category.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.pecunia.category.application.exception.CategoryNotFoundException;
import com.pecunia.category.application.port.in.GetCategoryQuery;
import com.pecunia.category.application.port.out.CategoryRepository;
import com.pecunia.category.application.readmodel.CategoryView;
import com.pecunia.category.domain.Category;
import com.pecunia.category.domain.CategoryType;
import com.pecunia.category.domain.HexColor;
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
class GetCategoryServiceTest {

    private static final UserId OWNER = UserId.of(UUID.randomUUID());
    private static final CategoryId CATEGORY_ID = CategoryId.of(UUID.randomUUID());
    private static final CategoryId PARENT_ID = CategoryId.of(UUID.randomUUID());
    private static final HexColor COLOR = new HexColor("#33AA55");

    @Mock
    private CategoryRepository categoryRepository;

    private GetCategoryService service;

    @BeforeEach
    void setUp() {
        service = new GetCategoryService(categoryRepository);
    }

    @Test
    @DisplayName("maps every aggregate field onto the returned CategoryView")
    void returns_category_view() {
        // given
        GetCategoryQuery query = new GetCategoryQuery(OWNER, CATEGORY_ID);
        Category category = Category.create(
                CATEGORY_ID, OWNER, CategoryType.EXPENSE, "Groceries", COLOR, "shopping_cart", PARENT_ID, 3);
        when(categoryRepository.findByIdAndOwner(CATEGORY_ID, OWNER)).thenReturn(Optional.of(category));

        // when
        CategoryView view = service.getById(query);

        // then
        assertThat(view.id()).isEqualTo(CATEGORY_ID);
        assertThat(view.name()).isEqualTo("Groceries");
        assertThat(view.type()).isEqualTo(CategoryType.EXPENSE);
        assertThat(view.color()).isEqualTo(COLOR);
        assertThat(view.icon()).contains("shopping_cart");
        assertThat(view.displayOrder()).isEqualTo(3);
        assertThat(view.archived()).isFalse();
        assertThat(view.parent()).contains(PARENT_ID);
    }

    @Test
    @DisplayName("throws CategoryNotFoundException when the category is absent or not owned")
    void rejects_missing_category() {
        // given
        GetCategoryQuery query = new GetCategoryQuery(OWNER, CATEGORY_ID);
        when(categoryRepository.findByIdAndOwner(CATEGORY_ID, OWNER)).thenReturn(Optional.empty());

        // when + then
        assertThatThrownBy(() -> service.getById(query)).isInstanceOf(CategoryNotFoundException.class);
    }
}
