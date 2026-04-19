package com.example.algashop.ordering.domain.model.customer;

import com.example.algashop.ordering.domain.model.DomainException;
import com.example.algashop.ordering.domain.model.ErrorMessages;

public class CustomerArchivedException extends DomainException {

    public CustomerArchivedException() {
        super(ErrorMessages.ERROR_CUSTOMER_ARCHIVED);
    }

    public CustomerArchivedException(Throwable cause) {
        super(ErrorMessages.ERROR_CUSTOMER_ARCHIVED, cause);
    }
}
