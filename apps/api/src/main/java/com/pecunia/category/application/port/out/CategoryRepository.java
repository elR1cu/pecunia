package com.pecunia.category.application.port.out;

import com.pecunia.category.domain.Category;
import com.pecunia.sharedkernel.CategoryId;
import com.pecunia.sharedkernel.UserId;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Driven port for category persistence.
 *
 * <p>Multi-tenant by design: {@code owner} is forced into every read signature
 * so a query cannot forget the ownership filter — the port guarantees
 * isolation, not the developer's vigilance (see {@code AccountRepository} and
 * the multi-tenancy rules in CLAUDE.md).
 */
public interface CategoryRepository {

    void save(Category category);

    Optional<Category> findByIdAndOwner(CategoryId id, UserId owner);

    List<Category> findAllByOwner(UserId owner);

    /**
     * Ids of all ancestors of {@code id} (its parent, grandparent, …), walked up
     * the hierarchy. Backed by a recursive CTE in the adapter. Used to reject a
     * reparenting that would create a cycle: moving X under P is a cycle iff X is
     * among the ancestors of P. Returns an empty set for a root or an unknown id.
     */
    Set<CategoryId> findAncestorIds(CategoryId id, UserId owner);
}
