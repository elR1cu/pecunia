package com.pecunia.category.application.port.in;

import com.pecunia.sharedkernel.CategoryId;
import java.util.Optional;

/**
 * Input for {@link MoveCategoryToParent}. An empty {@code newParent} detaches the
 * category, promoting it to a root.
 */
public record MoveCategoryToParentCommand(CategoryId categoryId, Optional<CategoryId> newParent) {}
