package com.pecunia.category.application.port.in;

import com.pecunia.sharedkernel.CategoryId;
import com.pecunia.sharedkernel.UserId;

/** Input for {@link ReorderCategory}. */
public record ReorderCategoryCommand(UserId owner, CategoryId categoryId, int newDisplayOrder) {}
