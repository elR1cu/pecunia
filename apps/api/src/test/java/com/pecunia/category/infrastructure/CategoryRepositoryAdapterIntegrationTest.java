package com.pecunia.category.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.pecunia.category.application.port.out.CategoryRepository;
import com.pecunia.category.domain.Category;
import com.pecunia.category.domain.CategoryType;
import com.pecunia.category.domain.HexColor;
import com.pecunia.sharedkernel.CategoryId;
import com.pecunia.sharedkernel.UserId;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Integration tests for the category persistence adapter against a real
 * PostgreSQL (Flyway V1..V4 applied): the aggregate ↔ entity round-trip
 * (including the optional icon/parent and the optimistic-lock version) and the
 * {@code findAncestorIds} recursive CTE — native SQL that only a real database
 * can exercise. Cross-user isolation is covered separately in
 * {@link CategoryCrossUserIsolationIntegrationTest}.
 */
@Testcontainers
@DataJpaTest
@Import(CategoryRepositoryAdapter.class)
@TestPropertySource(properties = {"spring.jpa.hibernate.ddl-auto=validate", "spring.test.database.replace=none"})
class CategoryRepositoryAdapterIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:18");

    @Autowired
    private CategoryRepository categoryRepository;

    private static Category expenseCategory(CategoryId id, UserId owner, String name, CategoryId parent) {
        return Category.create(id, owner, CategoryType.EXPENSE, name, new HexColor("#1A2B3C"), null, parent, 0);
    }

    @Test
    @DisplayName("save + findByIdAndOwner round-trips the full aggregate state")
    void round_trips_a_full_category() {
        // given — a child carrying every optional field (icon, parent)
        UserId owner = UserId.of(UUID.randomUUID());
        CategoryId parentId = CategoryId.of(UUID.randomUUID());
        CategoryId childId = CategoryId.of(UUID.randomUUID());
        categoryRepository.save(expenseCategory(parentId, owner, "Food", null));
        categoryRepository.save(Category.create(
                childId,
                owner,
                CategoryType.EXPENSE,
                "Groceries",
                new HexColor("#1A2B3C"),
                "shopping_cart",
                parentId,
                2));

        // when
        Optional<Category> reloaded = categoryRepository.findByIdAndOwner(childId, owner);

        // then
        assertThat(reloaded).hasValueSatisfying(category -> {
            assertThat(category.id()).isEqualTo(childId);
            assertThat(category.owner()).isEqualTo(owner);
            assertThat(category.type()).isEqualTo(CategoryType.EXPENSE);
            assertThat(category.name()).isEqualTo("Groceries");
            assertThat(category.color()).isEqualTo(new HexColor("#1A2B3C"));
            assertThat(category.icon()).contains("shopping_cart");
            assertThat(category.parent()).contains(parentId);
            assertThat(category.displayOrder()).isEqualTo(2);
            assertThat(category.archived()).isFalse();
            assertThat(category.version()).contains(0L);
        });
    }

    @Test
    @DisplayName("saving a reloaded category updates it and bumps the optimistic-lock version")
    void increments_version_on_update() {
        // given — a persisted category, reloaded through the port (merge path)
        UserId owner = UserId.of(UUID.randomUUID());
        CategoryId id = CategoryId.of(UUID.randomUUID());
        categoryRepository.save(expenseCategory(id, owner, "Food", null));
        Category category = categoryRepository.findByIdAndOwner(id, owner).orElseThrow();

        // when
        category.rename("Restaurants");
        categoryRepository.save(category);

        // then
        assertThat(categoryRepository.findByIdAndOwner(id, owner)).hasValueSatisfying(reloaded -> {
            assertThat(reloaded.name()).isEqualTo("Restaurants");
            assertThat(reloaded.version()).contains(1L);
        });
    }

    @Test
    @DisplayName("findAncestorIds walks the chain up to the root, excluding the category itself")
    void returns_all_ancestors_of_a_deep_chain() {
        // given — Root → Grandparent → Parent → Child
        UserId owner = UserId.of(UUID.randomUUID());
        CategoryId rootId = CategoryId.of(UUID.randomUUID());
        CategoryId grandparentId = CategoryId.of(UUID.randomUUID());
        CategoryId parentId = CategoryId.of(UUID.randomUUID());
        CategoryId childId = CategoryId.of(UUID.randomUUID());
        categoryRepository.save(expenseCategory(rootId, owner, "Root", null));
        categoryRepository.save(expenseCategory(grandparentId, owner, "Grandparent", rootId));
        categoryRepository.save(expenseCategory(parentId, owner, "Parent", grandparentId));
        categoryRepository.save(expenseCategory(childId, owner, "Child", parentId));

        // when
        Set<CategoryId> ancestors = categoryRepository.findAncestorIds(childId, owner);

        // then — every ancestor, nothing else (not the child, no null for the root's parent)
        assertThat(ancestors).containsExactlyInAnyOrder(parentId, grandparentId, rootId);
    }

    @Test
    @DisplayName("findAncestorIds returns an empty set for a root category")
    void returns_empty_for_a_root() {
        // given
        UserId owner = UserId.of(UUID.randomUUID());
        CategoryId rootId = CategoryId.of(UUID.randomUUID());
        categoryRepository.save(expenseCategory(rootId, owner, "Root", null));

        // when
        // then
        assertThat(categoryRepository.findAncestorIds(rootId, owner)).isEmpty();
    }

    @Test
    @DisplayName("findAncestorIds returns an empty set for an unknown id")
    void returns_empty_for_an_unknown_id() {
        // given — nothing persisted for this id
        UserId owner = UserId.of(UUID.randomUUID());
        CategoryId unknownId = CategoryId.of(UUID.randomUUID());

        // when
        // then
        assertThat(categoryRepository.findAncestorIds(unknownId, owner)).isEmpty();
    }
}
