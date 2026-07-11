package com.pecunia.category.application.port.in;

/**
 * Driving port: rename a category. Throws {@code CategoryNotFoundException} when
 * the category is absent or not owned by the requester.
 */
public interface RenameCategory {

    void rename(RenameCategoryCommand command);
}
