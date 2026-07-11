package com.pecunia.category.application.service;

import com.pecunia.category.application.exception.CategoryNotFoundException;
import com.pecunia.category.application.port.in.ArchiveCategory;
import com.pecunia.category.application.port.in.ArchiveCategoryCommand;
import com.pecunia.category.application.port.out.CategoryRepository;
import com.pecunia.category.domain.Category;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ArchiveCategoryService implements ArchiveCategory {

    private final CategoryRepository categoryRepository;

    @Override
    @Transactional
    public void archive(ArchiveCategoryCommand command) {
        Category category = categoryRepository
                .findByIdAndOwner(command.categoryId(), command.owner())
                .orElseThrow(() -> new CategoryNotFoundException(command.categoryId()));
        category.archive();
        categoryRepository.save(category);
    }
}
