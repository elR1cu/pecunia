package com.pecunia.category.application.port.in;

import com.pecunia.sharedkernel.UserId;

/** Input for {@link ListCategories}. */
public record ListCategoriesQuery(UserId owner) {}
