package com.example.algashop.ordering.domain.exception;

public class ErrorMessages {

    public static final String VALIDATION_ERROR_EMAIL_IS_INVALID = "Email is invalid";
    public static final String VALIDATION_ERROR_PHONE_IS_INVALID = "Phone is invalid";
    public static final String VALIDATION_ERROR_BIRTHDATE_FROM_FUTURE = "BirthDate must be a past date";
    public static final String VALIDATION_ERROR_LOYALTY_POINTS_IS_NEGATIVE = "Loyalty points cannot be negative";
    public static final String VALIDATION_ERROR_LOYALTY_POINTS_IS_ZERO_OR_NEGATIVE = "Loyalty points cannot be zero or negative";
    public static final String VALIDATION_ERROR_ZIPCODE_LENGTH_INVALID = "Zip code value must be 5 characters long";

    public static final String ERROR_CUSTOMER_ARCHIVED = "Customer is archived it cannot be changed";

    private ErrorMessages() {
    }
}
