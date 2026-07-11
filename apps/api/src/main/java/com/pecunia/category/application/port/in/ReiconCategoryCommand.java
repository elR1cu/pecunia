package com.pecunia.category.application.port.in;

import com.pecunia.sharedkernel.CategoryId;
import java.util.Optional;

/**
 * Input for {@link ReiconCategory}. An empty {@code newIcon} clears the icon
 * (icons are optional).
 */
public record ReiconCategoryCommand(CategoryId categoryId, Optional<String> newIcon) {}
