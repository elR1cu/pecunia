package com.pecunia.category.application.exception;

import com.pecunia.sharedkernel.CategoryId;

/**
 * The category being acted upon does not exist <em>or</em> is not owned by the
 * requesting user. Both collapse into "not found" and map to HTTP 404 (never
 * 403), so the existence of another user's resource is not leaked. An
 * application concern (repository lookup), not a domain invariant — mirrors
 * {@code AccountNotFoundException}. See ADR-0027.
 */
public final class CategoryNotFoundException extends RuntimeException {

    public CategoryNotFoundException(CategoryId id) {
        super("Category not found: " + id);
    }
}
