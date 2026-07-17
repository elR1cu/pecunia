package com.pecunia.category.web;

import com.pecunia.category.application.exception.CategoryCycleException;
import com.pecunia.category.application.exception.CategoryNotFoundException;
import com.pecunia.category.application.exception.CategoryTypeMismatchException;
import com.pecunia.category.application.exception.InvalidParentCategoryException;
import com.pecunia.category.domain.exception.ArchivedCategoryModificationException;
import com.pecunia.category.domain.exception.CategoryAlreadyArchivedException;
import com.pecunia.category.domain.exception.CategoryCannotBeItsOwnParentException;
import com.pecunia.sharedkernel.exception.DomainException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Maps category exceptions to HTTP statuses per ADR-0034. Scoped to this
 * package (basePackageClasses) so it never intercepts another context's
 * exceptions, and vice versa.
 *
 * <p>Precedence matters: Spring resolves the most specific handler, so the
 * {@code CONFLICT} handlers for the archived-state and self-parent domain
 * exceptions win over the generic {@link DomainException} handler that would
 * otherwise send every domain invariant to 422.
 */
@RestControllerAdvice(basePackageClasses = CategoryRestControllerAdvice.class)
class CategoryRestControllerAdvice extends ResponseEntityExceptionHandler {

    // --- 404: the URL resource is invisible to the current user -----------------

    @ExceptionHandler(CategoryNotFoundException.class)
    public ProblemDetail handleCategoryNotFoundException(CategoryNotFoundException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
    }

    // --- 409: conflict with the resource's current state ------------------------

    @ExceptionHandler(CategoryCycleException.class)
    public ProblemDetail handleCategoryCycleException(CategoryCycleException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.getMessage());
    }

    @ExceptionHandler(ArchivedCategoryModificationException.class)
    public ProblemDetail handleArchivedCategoryModificationException(ArchivedCategoryModificationException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.getMessage());
    }

    @ExceptionHandler(CategoryAlreadyArchivedException.class)
    public ProblemDetail handleCategoryAlreadyArchivedException(CategoryAlreadyArchivedException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.getMessage());
    }

    // Degenerate self-cycle: unreachable through the current use cases (the move
    // service rejects a self-parent as a CategoryCycleException first), but
    // mapped defensively to 409 so it never falls through to 422 as a cycle.
    @ExceptionHandler(CategoryCannotBeItsOwnParentException.class)
    public ProblemDetail handleCategoryCannotBeItsOwnParentException(CategoryCannotBeItsOwnParentException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.getMessage());
    }

    // --- 422: well-formed but semantically unprocessable ------------------------

    @ExceptionHandler(InvalidParentCategoryException.class)
    public ProblemDetail handleInvalidParentCategoryException(InvalidParentCategoryException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_CONTENT, e.getMessage());
    }

    @ExceptionHandler(CategoryTypeMismatchException.class)
    public ProblemDetail handleCategoryTypeMismatchException(CategoryTypeMismatchException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_CONTENT, e.getMessage());
    }

    // Catches the remaining domain invariants (e.g. InvalidHexColorException).
    @ExceptionHandler(DomainException.class)
    public ProblemDetail handleDomainException(DomainException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_CONTENT, e.getMessage());
    }
}
