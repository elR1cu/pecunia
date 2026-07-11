package com.pecunia.category.application.port.in;

/**
 * Driving port: change (or clear) a category's icon. Throws
 * {@code CategoryNotFoundException} when the category is absent or not owned.
 */
public interface ReiconCategory {

    void reicon(ReiconCategoryCommand command);
}
