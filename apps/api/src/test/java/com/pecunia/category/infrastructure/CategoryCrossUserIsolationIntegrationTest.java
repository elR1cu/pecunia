package com.pecunia.category.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.pecunia.category.application.port.out.CategoryRepository;
import com.pecunia.category.domain.Category;
import com.pecunia.category.domain.CategoryType;
import com.pecunia.category.domain.HexColor;
import com.pecunia.sharedkernel.CategoryId;
import com.pecunia.sharedkernel.UserId;
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
 * Cross-user isolation test for the category persistence adapter against a
 * real PostgreSQL (Flyway V1..V4 applied). Mandatory from Block 2 (ADR-0014):
 * a user must never reach another user's category through the repository. The
 * {@link CategoryRepository} port bakes the {@code owner} into every read —
 * including the {@code findAncestorIds} recursive CTE, whose anchor and
 * recursive members both filter by owner (defense in depth, ADR-0031).
 */
@Testcontainers
@DataJpaTest
@Import(CategoryRepositoryAdapter.class)
@TestPropertySource(properties = {"spring.jpa.hibernate.ddl-auto=validate", "spring.test.database.replace=none"})
class CategoryCrossUserIsolationIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:18");

    @Autowired
    private CategoryRepository categoryRepository;

    private static Category expenseCategory(CategoryId id, UserId owner, String name, CategoryId parent) {
        return Category.create(id, owner, CategoryType.EXPENSE, name, new HexColor("#1A2B3C"), null, parent, 0);
    }

    @Test
    @DisplayName("findByIdAndOwner returns the category for its owner")
    void finds_category_for_its_owner() {
        // given
        UserId owner = UserId.of(UUID.randomUUID());
        CategoryId categoryId = CategoryId.of(UUID.randomUUID());
        categoryRepository.save(expenseCategory(categoryId, owner, "Groceries", null));

        // when
        // then
        assertThat(categoryRepository.findByIdAndOwner(categoryId, owner))
                .get()
                .extracting(Category::id)
                .isEqualTo(categoryId);
    }

    @Test
    @DisplayName("findByIdAndOwner returns empty when another user requests the category")
    void does_not_leak_category_to_another_user() {
        // given — user A owns a category
        UserId userA = UserId.of(UUID.randomUUID());
        UserId userB = UserId.of(UUID.randomUUID());
        CategoryId categoryId = CategoryId.of(UUID.randomUUID());
        categoryRepository.save(expenseCategory(categoryId, userA, "Groceries", null));

        // when — user B asks for A's category by its id
        // then — the row is invisible to B (the use case maps this to 404)
        assertThat(categoryRepository.findByIdAndOwner(categoryId, userB)).isEmpty();
    }

    @Test
    @DisplayName("findAllByOwner returns only the caller's own categories")
    void lists_only_own_categories() {
        // given — one category per user
        UserId userA = UserId.of(UUID.randomUUID());
        UserId userB = UserId.of(UUID.randomUUID());
        CategoryId categoryOfA = CategoryId.of(UUID.randomUUID());
        CategoryId categoryOfB = CategoryId.of(UUID.randomUUID());
        categoryRepository.save(expenseCategory(categoryOfA, userA, "Groceries", null));
        categoryRepository.save(expenseCategory(categoryOfB, userB, "Rent", null));

        // when
        // then — B sees its own category only, never A's
        assertThat(categoryRepository.findAllByOwner(userB))
                .extracting(Category::id)
                .containsExactly(categoryOfB);
    }

    @Test
    @DisplayName("findAncestorIds returns empty when another user walks the hierarchy")
    void does_not_walk_another_users_hierarchy() {
        // given — user A owns Parent → Child
        UserId userA = UserId.of(UUID.randomUUID());
        UserId userB = UserId.of(UUID.randomUUID());
        CategoryId parentOfA = CategoryId.of(UUID.randomUUID());
        CategoryId childOfA = CategoryId.of(UUID.randomUUID());
        categoryRepository.save(expenseCategory(parentOfA, userA, "Food", null));
        categoryRepository.save(expenseCategory(childOfA, userA, "Groceries", parentOfA));

        // when — user B asks for the ancestors of A's child
        // then — the anchor is owner-filtered: empty, indistinguishable from an unknown id
        assertThat(categoryRepository.findAncestorIds(childOfA, userB)).isEmpty();
    }

    @Test
    @DisplayName("findAncestorIds stops walking at the tenant boundary")
    void stops_the_ancestor_walk_at_the_tenant_boundary() {
        // given — user A owns Root → Parent, and user B's category points at A's
        // Parent: a state ParentCategoryValidator normally forbids, persisted here
        // on purpose to prove the CTE's recursive member is the last line of defense
        UserId userA = UserId.of(UUID.randomUUID());
        UserId userB = UserId.of(UUID.randomUUID());
        CategoryId rootOfA = CategoryId.of(UUID.randomUUID());
        CategoryId parentOfA = CategoryId.of(UUID.randomUUID());
        CategoryId childOfB = CategoryId.of(UUID.randomUUID());
        categoryRepository.save(expenseCategory(rootOfA, userA, "Root", null));
        categoryRepository.save(expenseCategory(parentOfA, userA, "Parent", rootOfA));
        categoryRepository.save(expenseCategory(childOfB, userB, "Child", parentOfA));

        // when — B walks the ancestors of its own category
        Set<CategoryId> ancestors = categoryRepository.findAncestorIds(childOfB, userB);

        // then — only the direct parent id (already stored in B's own row) comes
        // back; the walk never enters A's hierarchy, so rootOfA stays invisible
        assertThat(ancestors).containsExactly(parentOfA);
    }
}
