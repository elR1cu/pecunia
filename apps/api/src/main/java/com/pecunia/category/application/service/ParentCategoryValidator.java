package com.pecunia.category.application.service;

import com.pecunia.category.application.exception.CategoryTypeMismatchException;
import com.pecunia.category.application.exception.InvalidParentCategoryException;
import com.pecunia.category.application.port.out.CategoryRepository;
import com.pecunia.category.domain.Category;
import com.pecunia.category.domain.CategoryType;
import com.pecunia.sharedkernel.CategoryId;
import com.pecunia.sharedkernel.UserId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class ParentCategoryValidator {

    private final CategoryRepository categoryRepository;

    public void validate(CategoryId parentId, UserId owner, CategoryType childType) {
        Category newParent = categoryRepository
                .findByIdAndOwner(parentId, owner)
                .orElseThrow(() -> new InvalidParentCategoryException(parentId));

        if (newParent.archived()) {
            throw new InvalidParentCategoryException(parentId);
        }

        if (newParent.type() != childType) {
            throw new CategoryTypeMismatchException(childType, newParent.type());
        }
    }
}
