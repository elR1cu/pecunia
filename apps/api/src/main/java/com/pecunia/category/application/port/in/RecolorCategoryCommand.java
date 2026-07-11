package com.pecunia.category.application.port.in;

import com.pecunia.category.domain.HexColor;
import com.pecunia.sharedkernel.CategoryId;
import com.pecunia.sharedkernel.UserId;

/** Input for {@link RecolorCategory}. */
public record RecolorCategoryCommand(UserId owner, CategoryId categoryId, HexColor newColor) {}
