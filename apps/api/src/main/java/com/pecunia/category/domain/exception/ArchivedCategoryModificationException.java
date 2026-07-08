package com.pecunia.category.domain.exception;

import com.pecunia.sharedkernel.CategoryId;
import com.pecunia.sharedkernel.exception.DomainException;

public class ArchivedCategoryModificationException extends DomainException {
    public ArchivedCategoryModificationException(CategoryId id) {
        super("Category with id '%s' cannot be modified because it is archived.".formatted(id));
    }
}
