package com.pecunia.account.web;

import com.pecunia.account.application.exception.AccountNotFoundException;
import com.pecunia.account.domain.exception.AccountAlreadyArchivedException;
import com.pecunia.shared.exception.DomainException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice(basePackageClasses = AccountRestControllerAdvice.class)
class AccountRestControllerAdvice extends ResponseEntityExceptionHandler {

    @ExceptionHandler(AccountNotFoundException.class)
    public ProblemDetail handleAccountNotFoundException(AccountNotFoundException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(AccountAlreadyArchivedException.class)
    public ProblemDetail handleAccountAlreadyArchivedException(AccountAlreadyArchivedException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.getMessage());
    }

    @ExceptionHandler(DomainException.class)
    public ProblemDetail handleDomainException(DomainException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
    }
}
