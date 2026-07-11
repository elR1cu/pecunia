package com.pecunia.category.application.port.in;

/**
 * Driving port: archive a category. Throws {@code CategoryNotFoundException} when
 * the category is absent or not owned; the aggregate rejects a double archive.
 */
public interface ArchiveCategory {

    void archive(ArchiveCategoryCommand command);
}
