package com.pecunia.category.infrastructure;

import com.pecunia.category.application.port.out.CategoryRepository;
import com.pecunia.category.domain.Category;
import com.pecunia.sharedkernel.CategoryId;
import com.pecunia.sharedkernel.UserId;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CategoryRepositoryAdapter implements CategoryRepository {

    private final CategoryJpaRepository categoryJpaRepository;

    @Override
    public void save(Category category) {
        categoryJpaRepository.save(CategoryEntity.fromDomain(category));
    }

    @Override
    public Optional<Category> findByIdAndOwner(CategoryId id, UserId owner) {
        return categoryJpaRepository
                .findByIdAndOwnerId(id.value(), owner.value())
                .map(CategoryEntity::toDomain);
    }

    @Override
    public List<Category> findAllByOwner(UserId owner) {
        return categoryJpaRepository.findAllByOwnerId(owner.value()).stream()
                .map(CategoryEntity::toDomain)
                .toList();
    }

    @Override
    public Set<CategoryId> findAncestorIds(CategoryId id, UserId owner) {
        throw new UnsupportedOperationException("findAncestorIds: recursive CTE pending.");
    }
}
