package com.pecunia.category.application.service;

import com.pecunia.category.application.exception.CategoryNotFoundException;
import com.pecunia.category.application.port.in.GetCategory;
import com.pecunia.category.application.port.in.GetCategoryQuery;
import com.pecunia.category.application.port.out.CategoryRepository;
import com.pecunia.category.application.readmodel.CategoryView;
import com.pecunia.category.domain.Category;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GetCategoryService implements GetCategory {

    private final CategoryRepository categoryRepository;

    @Override
    @Transactional(readOnly = true)
    public CategoryView getById(GetCategoryQuery query) {
        return categoryRepository
                .findByIdAndOwner(query.categoryId(), query.owner())
                .map(this::toCategoryView)
                .orElseThrow(() -> new CategoryNotFoundException(query.categoryId()));
    }

    private CategoryView toCategoryView(Category cat) {
        return new CategoryView(
                cat.id(),
                cat.name(),
                cat.type(),
                cat.color(),
                cat.icon(),
                cat.displayOrder(),
                cat.archived(),
                cat.parent());
    }
}
