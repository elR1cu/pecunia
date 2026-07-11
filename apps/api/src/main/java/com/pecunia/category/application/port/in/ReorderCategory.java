package com.pecunia.category.application.port.in;

/**
 * Driving port: change a category's display order. Throws
 * {@code CategoryNotFoundException} when the category is absent or not owned.
 */
public interface ReorderCategory {

    void reorder(ReorderCategoryCommand command);
}
