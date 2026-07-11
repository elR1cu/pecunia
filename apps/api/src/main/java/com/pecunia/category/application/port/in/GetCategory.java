package com.pecunia.category.application.port.in;

import com.pecunia.category.application.readmodel.CategoryView;

/**
 * Driving port: a single category as a flat read model. Used by the web layer to
 * build the 201 body after a create and to populate the edit dialog. Throws
 * {@code CategoryNotFoundException} when the category is absent or not owned.
 */
public interface GetCategory {

    CategoryView getById(GetCategoryQuery query);
}
