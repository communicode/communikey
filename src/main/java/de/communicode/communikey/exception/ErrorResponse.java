/*
 * This file is part of communikey.
 * Copyright (C) 2016-2018  communicode AG <communicode.de>
 *
 * communikey is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package de.communicode.communikey.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.google.common.collect.Lists;
import org.springframework.http.HttpStatus;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a error response.
 *
 * @author sgreb@communicode.de
 * @since 0.2.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

    private Timestamp timestamp;
    private int status;
    private String reason;
    private String error;
    private List<String> errors;

    /**
     * Constructs a new error response with the specified {@link HttpStatus} an a timestamp.
     *
     * @param httpStatus the HTTP status of the error
     * @param timestamp the timestamp of the error
     * @param error the error about this error
     */
    public ErrorResponse(HttpStatus httpStatus, Timestamp timestamp, String error) {
        status = httpStatus.value();
        reason = httpStatus.getReasonPhrase();
        this.timestamp = new Timestamp(timestamp.getTime());
        this.error = error;
    }

    /**
     * Constructs a new error response with the specified {@link HttpStatus}, a timestamp and a list of all errors.
     *
     * @param httpStatus the HTTP status of the error
     * @param timestamp the timestamp of the error
     * @param errors the list of errors
     */
    public ErrorResponse(HttpStatus httpStatus, Timestamp timestamp, List<String> errors) {
        status = httpStatus.value();
        reason = httpStatus.getReasonPhrase();
        this.timestamp = new Timestamp(timestamp.getTime());
        this.errors = errors;
    }

    public String getError() {
        return error;
    }

    public List<String> getErrors() {
        return errors;
    }

    public String getReason() {
        return reason;
    }

    public int getStatus() {
        return status;
    }

    public Timestamp getTimestamp() {
        return new Timestamp(timestamp.getTime());
    }

    public void setError(String error) {
        this.error = error;
    }

    public void setErrors(List<String> errors) {
        this.errors = errors;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = new Timestamp(timestamp.getTime());
    }
}
