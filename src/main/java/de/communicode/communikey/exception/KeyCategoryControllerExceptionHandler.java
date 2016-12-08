/*
 * Copyright (C) communicode AG - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * 2016
 */
package de.communicode.communikey.exception;

import de.communicode.communikey.controller.KeyCategoryController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import javax.servlet.http.HttpServletRequest;

import java.sql.Timestamp;
import java.util.Calendar;

/**
 * The exception handler for the {@link KeyCategoryController} that returns {@link ResponseEntity} of type {@link ErrorResponse}.
 *
 * @author sgreb@communicode.de
 * @since 0.2.0
 */
@ControllerAdvice
public class KeyCategoryControllerExceptionHandler extends GlobalControllerExceptionHandler {

    /**
     * Handles all exceptions of type {@link KeyCategoryNotFoundException}.
     *
     * @param exception the exception to handle
     * @param request the HTTP servlet request to extract data for the response entity of
     * @return the response entity
     */
    @ExceptionHandler(KeyCategoryNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleKeyCategoryNotFoundException(final KeyCategoryNotFoundException exception, HttpServletRequest request) {
        return createErrorResponse(HttpStatus.NOT_FOUND, new Timestamp(Calendar.getInstance().getTimeInMillis()), exception.getMessage(),
            request);
    }
}