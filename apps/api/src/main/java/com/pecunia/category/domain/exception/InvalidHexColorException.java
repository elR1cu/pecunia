package com.pecunia.category.domain.exception;

import com.pecunia.sharedkernel.exception.DomainException;

public class InvalidHexColorException extends DomainException {

    public InvalidHexColorException(String color) {
        super("Invalid hex color: " + color);
    }
}
