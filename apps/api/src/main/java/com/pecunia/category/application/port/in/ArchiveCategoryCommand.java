package com.pecunia.category.application.port.in;

import com.pecunia.sharedkernel.CategoryId;
import com.pecunia.sharedkernel.UserId;

/** Input for {@link ArchiveCategory}. */
public record ArchiveCategoryCommand(UserId owner, CategoryId categoryId) {}
