package com.pecunia.category.application.service;

import com.pecunia.category.application.exception.CategoryNotFoundException;
import com.pecunia.category.application.port.in.RecolorCategory;
import com.pecunia.category.application.port.in.RecolorCategoryCommand;
import com.pecunia.category.application.port.out.CategoryRepository;
import com.pecunia.category.domain.Category;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RecolorCategoryService implements RecolorCategory {

    private final CategoryRepository categoryRepository;

    @Override
    @Transactional
    public void recolor(RecolorCategoryCommand command) {
        Category category = categoryRepository
                .findByIdAndOwner(command.categoryId(), command.owner())
                .orElseThrow(() -> new CategoryNotFoundException(command.categoryId()));

        category.recolor(command.newColor());

        categoryRepository.save(category);
    }
}
