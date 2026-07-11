package com.pecunia.category.application.service;

import com.pecunia.category.application.exception.CategoryCycleException;
import com.pecunia.category.application.exception.CategoryNotFoundException;
import com.pecunia.category.application.port.in.MoveCategoryToParent;
import com.pecunia.category.application.port.in.MoveCategoryToParentCommand;
import com.pecunia.category.application.port.out.CategoryRepository;
import com.pecunia.category.domain.Category;
import com.pecunia.category.domain.CategoryType;
import com.pecunia.category.domain.exception.ArchivedCategoryModificationException;
import com.pecunia.sharedkernel.CategoryId;
import com.pecunia.sharedkernel.CurrentUserProvider;
import com.pecunia.sharedkernel.UserId;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MoveCategoryToParentService implements MoveCategoryToParent {

    private final CategoryRepository categoryRepository;
    private final ParentCategoryValidator parentCategoryValidator;
    private final CurrentUserProvider currentUserProvider;

    @Override
    @Transactional
    public void moveToParent(MoveCategoryToParentCommand command) {
        UserId owner = currentUserProvider.currentUserId();
        Category category = categoryRepository
                .findByIdAndOwner(command.categoryId(), owner)
                .orElseThrow(() -> new CategoryNotFoundException(command.categoryId()));

        if (category.archived()) {
            throw new ArchivedCategoryModificationException(category.id());
        }

        command.newParent()
                .ifPresentOrElse(
                        newParentId -> {
                            validateParent(newParentId, owner, category.type());
                            rejectCycle(category, newParentId, owner);
                            category.moveTo(newParentId);
                        },
                        category::detach);

        categoryRepository.save(category);
    }

    private void rejectCycle(Category category, CategoryId parentId, UserId owner) {
        if (parentId.equals(category.id())) {
            throw new CategoryCycleException(category.id(), parentId);
        }

        Set<CategoryId> parentAncestorIds = categoryRepository.findAncestorIds(parentId, owner);

        if (parentAncestorIds.contains(category.id())) {
            throw new CategoryCycleException(category.id(), parentId);
        }
    }

    private void validateParent(CategoryId parentId, UserId owner, CategoryType categoryType) {
        parentCategoryValidator.validate(parentId, owner, categoryType);
    }
}
