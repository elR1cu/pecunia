package com.pecunia.category.application.port.in;

import com.pecunia.sharedkernel.CategoryId;
import com.pecunia.sharedkernel.UserId;

/** Input for {@link RenameCategory}. */
public record RenameCategoryCommand(UserId owner, CategoryId categoryId, String newName) {}
