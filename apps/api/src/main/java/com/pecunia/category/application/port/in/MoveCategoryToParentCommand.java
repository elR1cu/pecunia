package com.pecunia.category.application.port.in;

import com.pecunia.sharedkernel.CategoryId;
import com.pecunia.sharedkernel.UserId;
import java.util.Optional;

/**
 * Input for {@link MoveCategoryToParent}. An empty {@code newParent} detaches the
 * category, promoting it to a root.
 */
public record MoveCategoryToParentCommand(UserId owner, CategoryId categoryId, Optional<CategoryId> newParent) {}
