package com.pecunia.category.application.exception;

import com.pecunia.category.domain.CategoryType;

/**
 * A category and its (would-be) parent must share the same {@link CategoryType}.
 * A cross-aggregate invariant: it spans two categories, so the application
 * service enforces it rather than the pure aggregate — hence it lives here, not
 * in the domain. Maps to HTTP 409 (web slice).
 */
public final class CategoryTypeMismatchException extends RuntimeException {

    public CategoryTypeMismatchException(CategoryType childType, CategoryType parentType) {
        super("Category type " + childType + " does not match parent type " + parentType);
    }
}
