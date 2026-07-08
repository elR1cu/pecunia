package com.pecunia.category.domain.exception;

import com.pecunia.sharedkernel.CategoryId;
import com.pecunia.sharedkernel.exception.DomainException;

public class CategoryCannotBeItsOwnParentException extends DomainException {
    public CategoryCannotBeItsOwnParentException(CategoryId id) {
        super("Category with id '%s' cannot be a parent of itself.".formatted(id));
    }
}
