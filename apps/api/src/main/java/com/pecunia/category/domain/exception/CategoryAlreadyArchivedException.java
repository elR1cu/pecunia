package com.pecunia.category.domain.exception;

import com.pecunia.sharedkernel.CategoryId;
import com.pecunia.sharedkernel.exception.DomainException;

public class CategoryAlreadyArchivedException extends DomainException {

    public CategoryAlreadyArchivedException(CategoryId id) {
        super("Category already archived: " + id);
    }
}
