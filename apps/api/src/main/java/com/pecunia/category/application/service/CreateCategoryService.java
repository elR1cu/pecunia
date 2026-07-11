package com.pecunia.category.application.service;

import com.pecunia.category.application.port.in.CreateCategory;
import com.pecunia.category.application.port.in.CreateCategoryCommand;
import com.pecunia.category.application.port.out.CategoryRepository;
import com.pecunia.category.domain.Category;
import com.pecunia.sharedkernel.CategoryId;
import com.pecunia.sharedkernel.IdGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CreateCategoryService implements CreateCategory {

    private final CategoryRepository categoryRepository;
    private final ParentCategoryValidator parentCategoryValidator;
    private final IdGenerator idGenerator;

    @Override
    @Transactional
    public CategoryId create(CreateCategoryCommand command) {
        command.parent()
                .ifPresent(parentId -> parentCategoryValidator.validate(parentId, command.owner(), command.type()));

        CategoryId id = CategoryId.of(idGenerator.newId());
        categoryRepository.save(Category.create(
                id,
                command.owner(),
                command.type(),
                command.name(),
                command.color(),
                command.icon().orElse(null),
                command.parent().orElse(null),
                command.displayOrder()));
        return id;
    }
}
