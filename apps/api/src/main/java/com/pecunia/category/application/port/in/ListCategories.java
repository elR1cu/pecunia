package com.pecunia.category.application.port.in;

import com.pecunia.category.application.readmodel.CategoryNode;
import java.util.List;

/**
 * Driving port: the owner's full category hierarchy as an immutable tree — the
 * roots, each carrying its ordered children. Returns a read model, never the
 * domain aggregate, so nothing outside the application can mutate a category.
 */
public interface ListCategories {

    List<CategoryNode> list(ListCategoriesQuery query);
}
