package com.pecunia.category.application.port.in;

/**
 * Driving port: reparent a category, or detach it to a root when the command's
 * parent is empty.
 *
 * <p>When a parent is supplied it is validated (existence, ownership, not
 * archived, same type) and a move that would create a cycle is rejected — the
 * cross-aggregate invariants the pure aggregate cannot see. Throws
 * {@code CategoryNotFoundException} / {@code InvalidParentCategoryException} /
 * {@code CategoryTypeMismatchException} / {@code CategoryCycleException}.
 *
 * <p>An archived category cannot be moved: the aggregate rejects it with
 * {@code ArchivedCategoryModificationException}.
 */
public interface MoveCategoryToParent {

    void moveToParent(MoveCategoryToParentCommand command);
}
