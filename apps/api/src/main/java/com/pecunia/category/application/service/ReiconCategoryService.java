package com.pecunia.category.application.service;

import com.pecunia.category.application.exception.CategoryNotFoundException;
import com.pecunia.category.application.port.in.ReiconCategory;
import com.pecunia.category.application.port.in.ReiconCategoryCommand;
import com.pecunia.category.application.port.out.CategoryRepository;
import com.pecunia.category.domain.Category;
import com.pecunia.sharedkernel.CurrentUserProvider;
import com.pecunia.sharedkernel.UserId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReiconCategoryService implements ReiconCategory {

    private final CategoryRepository categoryRepository;
    private final CurrentUserProvider currentUserProvider;

    @Override
    @Transactional
    public void reicon(ReiconCategoryCommand command) {
        UserId owner = currentUserProvider.currentUserId();
        Category category = categoryRepository
                .findByIdAndOwner(command.categoryId(), owner)
                .orElseThrow(() -> new CategoryNotFoundException(command.categoryId()));

        command.newIcon().ifPresentOrElse(category::reicon, category::clearIcon);

        categoryRepository.save(category);
    }
}
