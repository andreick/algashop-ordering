package com.example.algashop.ordering.domain.exception;

public class ErrorMessages {

    public static final String VALIDATION_ERROR_EMAIL_IS_INVALID = "Email is invalid";
    public static final String VALIDATION_ERROR_BIRTHDATE_IN_FUTURE = "BirthDate must be a past date";
    public static final String VALIDATION_ERROR_FULLNAME_IS_NULL = "FullName cannot be null";
    public static final String VALIDATION_ERROR_FULLNAME_IS_BLANK = "FullName cannot be blank";
    public static final String VALIDATION_ERROR_LOYALTY_POINTS_IS_NEGATIVE = "Loyalty points cannot be negative";

    public static final String ERROR_CUSTOMER_ARCHIVED = "Customer is archived it cannot be changed";

    private ErrorMessages() {
    }
}
