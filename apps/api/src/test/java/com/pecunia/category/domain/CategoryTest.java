package com.pecunia.category.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.pecunia.category.domain.exception.ArchivedCategoryModificationException;
import com.pecunia.category.domain.exception.CategoryAlreadyArchivedException;
import com.pecunia.category.domain.exception.CategoryCannotBeItsOwnParentException;
import com.pecunia.sharedkernel.CategoryId;
import com.pecunia.sharedkernel.UserId;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class CategoryTest {

    private static final CategoryId ID = CategoryId.of(UUID.randomUUID());
    private static final UserId OWNER = UserId.of(UUID.randomUUID());
    private static final HexColor COLOR = new HexColor("#1A2B3C");
    private static final CategoryId PARENT = CategoryId.of(UUID.randomUUID());
    private static final String ICON = "restaurant";

    private static Category activeCategory() {
        return Category.create(ID, OWNER, CategoryType.EXPENSE, "Food", COLOR, ICON, null, 0);
    }

    @Nested
    @DisplayName("create")
    class Create {

        @Test
        @DisplayName("creates an active root category carrying the given fields")
        void creates_active_root_category() {
            Category category = Category.create(ID, OWNER, CategoryType.EXPENSE, "Food", COLOR, ICON, null, 2);

            assertThat(category.id()).isEqualTo(ID);
            assertThat(category.owner()).isEqualTo(OWNER);
            assertThat(category.type()).isEqualTo(CategoryType.EXPENSE);
            assertThat(category.name()).isEqualTo("Food");
            assertThat(category.color()).isEqualTo(COLOR);
            assertThat(category.icon()).contains(ICON);
            assertThat(category.parent()).isEmpty();
            assertThat(category.displayOrder()).isEqualTo(2);
            assertThat(category.archived()).isFalse();
            assertThat(category.version()).isEmpty();
        }

        @Test
        @DisplayName("strips surrounding whitespace from the name")
        void strips_name() {
            Category category = Category.create(ID, OWNER, CategoryType.INCOME, "  Salary  ", COLOR, ICON, null, 0);

            assertThat(category.name()).isEqualTo("Salary");
        }

        @Test
        @DisplayName("carries the parent when one is given")
        void carries_parent() {
            Category category = Category.create(ID, OWNER, CategoryType.EXPENSE, "Groceries", COLOR, ICON, PARENT, 0);

            assertThat(category.parent()).contains(PARENT);
        }

        @Test
        @DisplayName("treats a null icon as absent")
        void null_icon_is_absent() {
            Category category = Category.create(ID, OWNER, CategoryType.EXPENSE, "Food", COLOR, null, null, 0);

            assertThat(category.icon()).isEmpty();
        }

        @Test
        @DisplayName("strips the icon when present")
        void strips_icon() {
            Category category = Category.create(ID, OWNER, CategoryType.EXPENSE, "Food", COLOR, "  leaf  ", null, 0);

            assertThat(category.icon()).contains("leaf");
        }

        @Test
        @DisplayName("rejects a null id")
        void rejects_null_id() {
            assertThatNullPointerException()
                    .isThrownBy(() -> Category.create(null, OWNER, CategoryType.EXPENSE, "Food", COLOR, ICON, null, 0));
        }

        @Test
        @DisplayName("rejects a null owner")
        void rejects_null_owner() {
            assertThatNullPointerException()
                    .isThrownBy(() -> Category.create(ID, null, CategoryType.EXPENSE, "Food", COLOR, ICON, null, 0));
        }

        @Test
        @DisplayName("rejects a null type")
        void rejects_null_type() {
            assertThatNullPointerException()
                    .isThrownBy(() -> Category.create(ID, OWNER, null, "Food", COLOR, ICON, null, 0));
        }

        @Test
        @DisplayName("rejects a null color")
        void rejects_null_color() {
            assertThatNullPointerException()
                    .isThrownBy(() -> Category.create(ID, OWNER, CategoryType.EXPENSE, "Food", null, ICON, null, 0));
        }

        @Test
        @DisplayName("rejects a null name")
        void rejects_null_name() {
            assertThatNullPointerException()
                    .isThrownBy(() -> Category.create(ID, OWNER, CategoryType.EXPENSE, null, COLOR, ICON, null, 0));
        }

        @Test
        @DisplayName("rejects a blank name")
        void rejects_blank_name() {
            assertThatThrownBy(() -> Category.create(ID, OWNER, CategoryType.EXPENSE, "   ", COLOR, ICON, null, 0))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("name cannot be blank");
        }

        @Test
        @DisplayName("rejects a negative display order")
        void rejects_negative_display_order() {
            assertThatThrownBy(() -> Category.create(ID, OWNER, CategoryType.EXPENSE, "Food", COLOR, ICON, null, -1))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Display order cannot be negative");
        }

        @Test
        @DisplayName("rejects a category that is its own parent")
        void rejects_self_parent() {
            assertThatThrownBy(() -> Category.create(ID, OWNER, CategoryType.EXPENSE, "Food", COLOR, ICON, ID, 0))
                    .isInstanceOf(CategoryCannotBeItsOwnParentException.class);
        }
    }

    @Nested
    @DisplayName("reconstitute")
    class Reconstitute {

        @Test
        @DisplayName("rehydrates the full persisted state, including archived and version")
        void rehydrates_full_state() {
            Category category =
                    Category.reconstitute(ID, OWNER, CategoryType.EXPENSE, "Food", COLOR, ICON, PARENT, 3, true, 7L);

            assertThat(category.parent()).contains(PARENT);
            assertThat(category.displayOrder()).isEqualTo(3);
            assertThat(category.archived()).isTrue();
            assertThat(category.version()).contains(7L);
        }
    }

    @Nested
    @DisplayName("mutations")
    class Mutations {

        @Test
        @DisplayName("renames and strips the new name")
        void renames_and_strips() {
            Category category = activeCategory();

            category.rename("  Rent  ");

            assertThat(category.name()).isEqualTo("Rent");
        }

        @Test
        @DisplayName("rejects a blank new name")
        void rename_rejects_blank() {
            Category category = activeCategory();

            assertThatThrownBy(() -> category.rename("   ")).isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("recolors the category")
        void recolors() {
            Category category = activeCategory();
            HexColor white = new HexColor("#FFFFFF");

            category.recolor(white);

            assertThat(category.color()).isEqualTo(white);
        }

        @Test
        @DisplayName("rejects a null new color")
        void recolor_rejects_null() {
            Category category = activeCategory();

            assertThatNullPointerException().isThrownBy(() -> category.recolor(null));
        }

        @Test
        @DisplayName("changes the icon, stripping it")
        void reicons_stripped() {
            Category category = activeCategory();

            category.reicon("  wallet  ");

            assertThat(category.icon()).contains("wallet");
        }

        @Test
        @DisplayName("clears the icon when given null")
        void reicon_clears_with_null() {
            Category category = activeCategory();

            category.reicon(null);

            assertThat(category.icon()).isEmpty();
        }

        @Test
        @DisplayName("reorders the category")
        void reorders() {
            Category category = activeCategory();

            category.reorder(5);

            assertThat(category.displayOrder()).isEqualTo(5);
        }

        @Test
        @DisplayName("rejects a negative display order")
        void reorder_rejects_negative() {
            Category category = activeCategory();

            assertThatThrownBy(() -> category.reorder(-1)).isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("archive")
    class Archive {

        @Test
        @DisplayName("archives an active category")
        void archives_active_category() {
            Category category = activeCategory();

            category.archive();

            assertThat(category.archived()).isTrue();
        }

        @Test
        @DisplayName("rejects archiving an already archived category")
        void rejects_double_archive() {
            Category category = activeCategory();
            category.archive();

            assertThatThrownBy(category::archive)
                    .isInstanceOf(CategoryAlreadyArchivedException.class)
                    .hasMessage("Category already archived: " + ID);
        }
    }

    @Nested
    @DisplayName("moveTo")
    class MoveTo {

        @Test
        @DisplayName("reparents the category")
        void reparents() {
            Category category = activeCategory();

            category.moveTo(PARENT);

            assertThat(category.parent()).contains(PARENT);
        }

        @Test
        @DisplayName("detaches to a root when moved to null")
        void detaches_to_root() {
            Category category = Category.create(ID, OWNER, CategoryType.EXPENSE, "Groceries", COLOR, ICON, PARENT, 0);

            category.moveTo(null);

            assertThat(category.parent()).isEmpty();
        }

        @Test
        @DisplayName("rejects moving a category under itself")
        void rejects_self_parent() {
            Category category = activeCategory();

            assertThatThrownBy(() -> category.moveTo(ID)).isInstanceOf(CategoryCannotBeItsOwnParentException.class);
        }
    }

    @Nested
    @DisplayName("modification of an archived category")
    class ModificationWhenArchived {

        private static final String EXPECTED_MESSAGE =
                "Category with id '%s' cannot be modified because it is archived.".formatted(ID);

        private Category archivedCategory() {
            Category category = activeCategory();
            category.archive();
            return category;
        }

        @Test
        @DisplayName("rejects renaming")
        void rejects_rename() {
            Category category = archivedCategory();

            assertThatThrownBy(() -> category.rename("Rent"))
                    .isInstanceOf(ArchivedCategoryModificationException.class)
                    .hasMessage(EXPECTED_MESSAGE);
        }

        @Test
        @DisplayName("rejects recoloring")
        void rejects_recolor() {
            Category category = archivedCategory();

            assertThatThrownBy(() -> category.recolor(COLOR)).isInstanceOf(ArchivedCategoryModificationException.class);
        }

        @Test
        @DisplayName("rejects changing the icon")
        void rejects_reicon() {
            Category category = archivedCategory();

            assertThatThrownBy(() -> category.reicon("wallet"))
                    .isInstanceOf(ArchivedCategoryModificationException.class);
        }

        @Test
        @DisplayName("rejects reordering")
        void rejects_reorder() {
            Category category = archivedCategory();

            assertThatThrownBy(() -> category.reorder(3)).isInstanceOf(ArchivedCategoryModificationException.class);
        }

        @Test
        @DisplayName("rejects reparenting")
        void rejects_move() {
            Category category = archivedCategory();

            assertThatThrownBy(() -> category.moveTo(PARENT)).isInstanceOf(ArchivedCategoryModificationException.class);
        }
    }

    @Nested
    @DisplayName("identity")
    class Identity {

        @Test
        @DisplayName("two categories with the same id are equal despite different state")
        void equal_by_id() {
            Category one = activeCategory();
            Category other = activeCategory();
            other.rename("Renamed");

            assertThat(one).isEqualTo(other).hasSameHashCodeAs(other);
        }

        @Test
        @DisplayName("two categories with different ids are not equal")
        void not_equal_by_different_id() {
            Category one = activeCategory();
            Category other = Category.create(
                    CategoryId.of(UUID.randomUUID()), OWNER, CategoryType.EXPENSE, "Food", COLOR, ICON, null, 0);

            assertThat(one).isNotEqualTo(other);
        }
    }
}
