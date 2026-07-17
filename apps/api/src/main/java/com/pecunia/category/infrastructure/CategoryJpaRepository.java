package com.pecunia.category.infrastructure;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CategoryJpaRepository extends JpaRepository<CategoryEntity, UUID> {

    Optional<CategoryEntity> findByIdAndOwnerId(UUID id, UUID ownerId);

    List<CategoryEntity> findAllByOwnerId(UUID ownerId);

    @Query(value = """
        WITH RECURSIVE walk AS (
            SELECT c1.parent_id
            FROM categories c1
            WHERE c1.id = :id AND c1.owner_id = :ownerId
            UNION
            SELECT c2.parent_id
            FROM categories c2
            JOIN walk ON walk.parent_id = c2.id
            WHERE c2.owner_id = :ownerId
        )
        SELECT parent_id FROM walk WHERE parent_id IS NOT NULL
        """, nativeQuery = true)
    List<UUID> findAncestorIds(@Param("id") UUID id, @Param("ownerId") UUID ownerId);
}
