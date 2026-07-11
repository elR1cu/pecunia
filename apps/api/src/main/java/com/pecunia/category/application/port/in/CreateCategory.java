package com.pecunia.category.application.port.in;

import com.pecunia.sharedkernel.CategoryId;

/**
 * Driving port: create a category for the owner.
 *
 * <p>Returns only the new {@link CategoryId} — a command yields metadata, never
 * a read model (CQRS). The web layer builds the 201 response body via
 * {@link GetCategory}. When a parent is supplied it is validated (existence,
 * ownership, not archived, same type); violations surface as
 * {@code InvalidParentCategoryException} / {@code CategoryTypeMismatchException}.
 * A create cannot form a cycle (the node is brand new, with no descendants).
 */
public interface CreateCategory {

    CategoryId create(CreateCategoryCommand command);
}
