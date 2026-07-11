package com.pecunia.category.application.port.in;

import com.pecunia.sharedkernel.CategoryId;
import com.pecunia.sharedkernel.UserId;

/** Input for {@link GetCategory}. */
public record GetCategoryQuery(UserId owner, CategoryId categoryId) {}
