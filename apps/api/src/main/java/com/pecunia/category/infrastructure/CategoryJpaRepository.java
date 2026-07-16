package com.pecunia.category.infrastructure;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryJpaRepository extends JpaRepository<CategoryEntity, UUID> {

    Optional<CategoryEntity> findByIdAndOwnerId(UUID id, UUID ownerId);

    List<CategoryEntity> findAllByOwnerId(UUID ownerId);
}
