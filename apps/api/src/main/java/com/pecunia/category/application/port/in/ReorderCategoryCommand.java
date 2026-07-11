package com.pecunia.category.application.port.in;

import com.pecunia.sharedkernel.CategoryId;

/** Input for {@link ReorderCategory}. */
public record ReorderCategoryCommand(CategoryId categoryId, int newDisplayOrder) {}
