package com.pecunia.category.application.port.in;

import com.pecunia.category.domain.HexColor;
import com.pecunia.sharedkernel.CategoryId;

/** Input for {@link RecolorCategory}. */
public record RecolorCategoryCommand(CategoryId categoryId, HexColor newColor) {}
