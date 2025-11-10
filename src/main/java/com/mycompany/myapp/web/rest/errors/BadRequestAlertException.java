package com.mycompany.myapp.web.rest.errors;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.ErrorResponseException;

@SuppressWarnings("java:S110") // Inheritance tree of classes should not be too deep
public class BadRequestAlertException extends ErrorResponseException {

    private static final long serialVersionUID = 1L;

    private final String entityName;

    private final String errorKey;

    public BadRequestAlertException(String defaultMessage, String entityName, String errorKey) {
        this(URI.create("/problem-with-message"), defaultMessage, entityName, errorKey);
    }

    public BadRequestAlertException(URI type, String defaultMessage, String entityName, String errorKey) {
        super(HttpStatus.BAD_REQUEST, createProblemDetail(type, defaultMessage), null);
        this.entityName = entityName;
        this.errorKey = errorKey;
    }

    public String getEntityName() {
        return entityName;
    }

    public String getErrorKey() {
        return errorKey;
    }

    private static org.springframework.http.ProblemDetail createProblemDetail(URI type, String defaultMessage) {
        org.springframework.http.ProblemDetail problemDetail = org.springframework.http.ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problemDetail.setType(type);
        problemDetail.setTitle(defaultMessage);
        return problemDetail;
    }
}
