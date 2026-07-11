package com.pecunia.category.application.service;

import com.pecunia.category.application.exception.CategoryNotFoundException;
import com.pecunia.category.application.port.in.RenameCategory;
import com.pecunia.category.application.port.in.RenameCategoryCommand;
import com.pecunia.category.application.port.out.CategoryRepository;
import com.pecunia.category.domain.Category;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RenameCategoryService implements RenameCategory {

    private final CategoryRepository categoryRepository;

    @Override
    @Transactional
    public void rename(RenameCategoryCommand command) {
        Category category = categoryRepository
                .findByIdAndOwner(command.categoryId(), command.owner())
                .orElseThrow(() -> new CategoryNotFoundException(command.categoryId()));

        category.rename(command.newName());

        categoryRepository.save(category);
    }
}
