package com.pecunia.category.application.readmodel;

import com.pecunia.category.domain.CategoryType;
import com.pecunia.category.domain.HexColor;
import com.pecunia.sharedkernel.CategoryId;
import java.util.Optional;

/**
 * Flat read model for a single category. Returned by query use cases so the
 * mutable {@link com.pecunia.category.domain.Category} aggregate never leaves
 * the application layer. Value objects are immutable, hence safe to expose.
 */
public record CategoryView(
        CategoryId id,
        String name,
        CategoryType type,
        HexColor color,
        Optional<String> icon,
        int displayOrder,
        boolean archived,
        Optional<CategoryId> parent) {}
