package com.pecunia.category.application.exception;

import com.pecunia.sharedkernel.CategoryId;

/**
 * The parent referenced by a create or move request cannot be used: it does not
 * exist, is not owned by the user, or is archived. The three reasons are
 * deliberately collapsed so as not to leak which one applies. A bad
 * client-supplied reference → maps to HTTP 400 (web slice), distinct from the
 * 404 of the target category itself.
 */
public final class InvalidParentCategoryException extends RuntimeException {

    public InvalidParentCategoryException(CategoryId parentId) {
        super("Invalid parent category: " + parentId);
    }
}
