package com.pecunia.category.application.port.in;

/**
 * Driving port: change a category's color. Throws
 * {@code CategoryNotFoundException} when the category is absent or not owned.
 */
public interface RecolorCategory {

    void recolor(RecolorCategoryCommand command);
}
