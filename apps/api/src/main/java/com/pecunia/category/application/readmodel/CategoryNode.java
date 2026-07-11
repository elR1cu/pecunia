package com.pecunia.category.application.readmodel;

import com.pecunia.category.domain.CategoryType;
import com.pecunia.category.domain.HexColor;
import com.pecunia.sharedkernel.CategoryId;
import java.util.List;
import java.util.Optional;

/**
 * Tree read model: a category together with its ordered children. Assembled in
 * memory by {@code ListCategories} from the owner's flat category set. The
 * parent link is intentionally omitted — the nesting already encodes it.
 */
public record CategoryNode(
        CategoryId id,
        String name,
        CategoryType type,
        HexColor color,
        Optional<String> icon,
        int displayOrder,
        boolean archived,
        List<CategoryNode> children) {}
