package com.pecunia.category.application.exception;

import com.pecunia.sharedkernel.CategoryId;

/**
 * Reparenting would create a cycle in the hierarchy: the requested parent is the
 * category itself or one of its descendants. A cross-aggregate invariant
 * enforced by the application service (which walks the ancestor chain via
 * {@code CategoryRepository.findAncestorIds}). Maps to HTTP 409 (web slice).
 */
public final class CategoryCycleException extends RuntimeException {

    public CategoryCycleException(CategoryId categoryId, CategoryId newParentId) {
        super("Moving category " + categoryId + " under " + newParentId + " would create a cycle");
    }
}
