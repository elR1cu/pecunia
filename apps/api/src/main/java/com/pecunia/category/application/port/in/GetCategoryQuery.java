package com.pecunia.category.application.port.in;

import com.pecunia.sharedkernel.CategoryId;

/** Input for {@link GetCategory}. */
public record GetCategoryQuery(CategoryId categoryId) {}
