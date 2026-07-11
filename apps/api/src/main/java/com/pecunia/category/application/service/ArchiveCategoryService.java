package com.pecunia.category.application.service;

import com.pecunia.category.application.exception.CategoryNotFoundException;
import com.pecunia.category.application.port.in.ArchiveCategory;
import com.pecunia.category.application.port.in.ArchiveCategoryCommand;
import com.pecunia.category.application.port.out.CategoryRepository;
import com.pecunia.category.domain.Category;
import com.pecunia.sharedkernel.CurrentUserProvider;
import com.pecunia.sharedkernel.UserId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ArchiveCategoryService implements ArchiveCategory {

    private final CategoryRepository categoryRepository;
    private final CurrentUserProvider currentUserProvider;

    @Override
    @Transactional
    public void archive(ArchiveCategoryCommand command) {
        UserId owner = currentUserProvider.currentUserId();
        Category category = categoryRepository
                .findByIdAndOwner(command.categoryId(), owner)
                .orElseThrow(() -> new CategoryNotFoundException(command.categoryId()));
        category.archive();
        categoryRepository.save(category);
    }
}
