package com.pecunia.category.application.port.in;

import com.pecunia.sharedkernel.CategoryId;

/** Input for {@link ArchiveCategory}. */
public record ArchiveCategoryCommand(CategoryId categoryId) {}
