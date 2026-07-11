package com.pecunia.category.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.pecunia.category.application.port.in.ListCategoriesQuery;
import com.pecunia.category.application.port.out.CategoryRepository;
import com.pecunia.category.application.readmodel.CategoryNode;
import com.pecunia.category.domain.Category;
import com.pecunia.category.domain.CategoryType;
import com.pecunia.category.domain.HexColor;
import com.pecunia.sharedkernel.CategoryId;
import com.pecunia.sharedkernel.UserId;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ListCategoriesServiceTest {

    private static final UserId OWNER = UserId.of(UUID.randomUUID());
    private static final HexColor COLOR = new HexColor("#33AA55");

    @Mock
    private CategoryRepository categoryRepository;

    private ListCategoriesService service;

    @BeforeEach
    void setUp() {
        service = new ListCategoriesService(categoryRepository);
    }

    private static Category category(
            CategoryId id, String name, CategoryType type, CategoryId parent, int displayOrder) {
        return Category.create(id, OWNER, type, name, COLOR, null, parent, displayOrder);
    }

    private static ListCategoriesQuery query() {
        return new ListCategoriesQuery(OWNER);
    }

    @Test
    @DisplayName("returns an empty list when the owner has no categories")
    void returns_empty_list() {
        // given
        when(categoryRepository.findAllByOwner(OWNER)).thenReturn(List.of());

        // when
        List<CategoryNode> roots = service.list(query());

        // then
        assertThat(roots).isEmpty();
    }

    @Test
    @DisplayName("nests children under their parent across several levels")
    void builds_tree() {
        // given
        CategoryId rootId = CategoryId.of(UUID.randomUUID());
        CategoryId childId = CategoryId.of(UUID.randomUUID());
        CategoryId grandchildId = CategoryId.of(UUID.randomUUID());
        Category root = category(rootId, "Household", CategoryType.EXPENSE, null, 0);
        Category child = category(childId, "Groceries", CategoryType.EXPENSE, rootId, 0);
        Category grandchild = category(grandchildId, "Vegetables", CategoryType.EXPENSE, childId, 0);
        // deliberately unordered: the flat list carries no hierarchy
        when(categoryRepository.findAllByOwner(OWNER)).thenReturn(List.of(grandchild, root, child));

        // when
        List<CategoryNode> roots = service.list(query());

        // then
        assertThat(roots).hasSize(1);
        CategoryNode rootNode = roots.getFirst();
        assertThat(rootNode.id()).isEqualTo(rootId);
        assertThat(rootNode.children()).hasSize(1);
        CategoryNode childNode = rootNode.children().getFirst();
        assertThat(childNode.id()).isEqualTo(childId);
        assertThat(childNode.children()).hasSize(1);
        CategoryNode grandchildNode = childNode.children().getFirst();
        assertThat(grandchildNode.id()).isEqualTo(grandchildId);
        assertThat(grandchildNode.children()).isEmpty();
    }

    @Test
    @DisplayName("orders the children of a node by display order")
    void orders_children_by_display_order() {
        // given
        CategoryId rootId = CategoryId.of(UUID.randomUUID());
        Category root = category(rootId, "Household", CategoryType.EXPENSE, null, 0);
        Category second = category(CategoryId.of(UUID.randomUUID()), "Insurance", CategoryType.EXPENSE, rootId, 2);
        Category first = category(CategoryId.of(UUID.randomUUID()), "Rent", CategoryType.EXPENSE, rootId, 1);
        when(categoryRepository.findAllByOwner(OWNER)).thenReturn(List.of(root, second, first));

        // when
        List<CategoryNode> roots = service.list(query());

        // then
        assertThat(roots.getFirst().children()).extracting(CategoryNode::name).containsExactly("Rent", "Insurance");
    }

    @Test
    @DisplayName("orders the roots by type, then by display order")
    void orders_roots_by_type_then_display_order() {
        // given
        Category salary = category(CategoryId.of(UUID.randomUUID()), "Salary", CategoryType.INCOME, null, 0);
        Category household = category(CategoryId.of(UUID.randomUUID()), "Household", CategoryType.EXPENSE, null, 1);
        Category transport = category(CategoryId.of(UUID.randomUUID()), "Transport", CategoryType.EXPENSE, null, 0);
        when(categoryRepository.findAllByOwner(OWNER)).thenReturn(List.of(salary, household, transport));

        // when
        List<CategoryNode> roots = service.list(query());

        // then
        assertThat(roots).extracting(CategoryNode::name).containsExactly("Transport", "Household", "Salary");
    }

    @Test
    @DisplayName("includes archived categories, exposing the archived flag")
    void includes_archived_categories() {
        // given
        Category active = category(CategoryId.of(UUID.randomUUID()), "Household", CategoryType.EXPENSE, null, 0);
        Category archived = category(CategoryId.of(UUID.randomUUID()), "Old hobby", CategoryType.EXPENSE, null, 1);
        archived.archive();
        when(categoryRepository.findAllByOwner(OWNER)).thenReturn(List.of(active, archived));

        // when
        List<CategoryNode> roots = service.list(query());

        // then
        assertThat(roots).extracting(CategoryNode::archived).containsExactly(false, true);
    }
}
