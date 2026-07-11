package com.pecunia.category.application.port.in;

import com.pecunia.sharedkernel.CategoryId;

/** Input for {@link RenameCategory}. */
public record RenameCategoryCommand(CategoryId categoryId, String newName) {}
