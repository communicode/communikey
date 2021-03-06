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
package de.communicode.communikey.controller;

import static de.communicode.communikey.controller.RequestMappings.AUTHORITIES;
import static de.communicode.communikey.controller.RequestMappings.AUTHORITIES_NAME;
import static java.util.Objects.requireNonNull;

import de.communicode.communikey.domain.Authority;
import de.communicode.communikey.exception.AuthorityNotFoundException;
import de.communicode.communikey.security.AuthoritiesConstants;
import de.communicode.communikey.service.AuthorityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;

/**
 * The REST API controller to process {@link Authority} entities.
 *
 * <p>Mapped to the "{@value RequestMappings#AUTHORITIES}" endpoint.
 *
 * @author sgreb@communicode.de
 * @since 0.3.0
 */
@RestController
@RequestMapping(AUTHORITIES)
public class AuthorityController {

    private final AuthorityService authorityService;

    @Autowired
    public AuthorityController(AuthorityService authorityService) {
        this.authorityService = requireNonNull(authorityService, "authorityService must not be null!");
    }

    /**
     * Gets the authority with the specified name.
     *
     * <p>This endpoint is mapped to "{@value RequestMappings#AUTHORITIES}{@value RequestMappings#AUTHORITIES_NAME}".
     *
     * @param authorityName the name of the authority to get
     * @return the authority as response entity
     * @throws AuthorityNotFoundException if the authority with the specified name has not been found
     */
    @GetMapping(value = AUTHORITIES_NAME)
    @Secured(AuthoritiesConstants.ADMIN)
    ResponseEntity<Authority> get(@PathVariable String authorityName) throws AuthorityNotFoundException {
        return new ResponseEntity<>(authorityService.get(authorityName), HttpStatus.OK);
    }

    /**
     * Gets all authorities.
     *
     * <p>This endpoint is mapped to "{@value RequestMappings#AUTHORITIES}".
     *
     * @return a collection of all authorities as response entity
     */
    @GetMapping
    @Secured(AuthoritiesConstants.ADMIN)
    ResponseEntity<Set<Authority>> getAll() {
        return new ResponseEntity<>(authorityService.getAll(), HttpStatus.OK);
    }
}
