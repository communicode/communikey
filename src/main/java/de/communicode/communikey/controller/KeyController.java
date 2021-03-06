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

import static de.communicode.communikey.controller.PathVariables.KEY_ID;
import static de.communicode.communikey.controller.RequestMappings.KEYS;
import static de.communicode.communikey.controller.RequestMappings.KEY_ENCRYPTED_PASSWORD;
import static de.communicode.communikey.controller.RequestMappings.KEY_HASHID;
import static de.communicode.communikey.controller.RequestMappings.KEY_SUBSCRIBERS;
import static java.util.Objects.requireNonNull;

import de.communicode.communikey.domain.Key;
import de.communicode.communikey.exception.HashidNotValidException;
import de.communicode.communikey.exception.KeyNotFoundException;
import de.communicode.communikey.exception.UserEncryptedPasswordNotFoundException;
import de.communicode.communikey.security.AuthoritiesConstants;
import de.communicode.communikey.service.payload.KeyPayload;
import de.communicode.communikey.service.KeyService;
import org.hashids.Hashids;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

import java.util.Set;

/**
 * The REST API controller to process {@link Key}s.
 *
 * <p>Mapped to the "{@value RequestMappings#KEYS}" endpoint.
 *
 * @author sgreb@communicode.de
 * @since 0.1.0
 */
@RestController
@RequestMapping(KEYS)
public class KeyController {
    private final KeyService keyService;
    private final Hashids hashids;

    @Autowired
    public KeyController(KeyService keyService, Hashids hashids) {
        this.keyService = requireNonNull(keyService, "keyService must not be null!");
        this.hashids = requireNonNull(hashids, "hashids must not be null!");
    }

    /**
     * Creates a new key with the specified payload.
     *
     * <p>This endpoint is mapped to "{@value RequestMappings#KEYS}{@value RequestMappings#KEY_HASHID}".
     *
     * @param payload the key request payload
     * @return the key as response entity
     * @since 0.2.0
     */
    @PostMapping
    @Secured(AuthoritiesConstants.ADMIN)
    public ResponseEntity<Key> create(@Valid @RequestBody KeyPayload payload) {
        return new ResponseEntity<>(keyService.create(payload), HttpStatus.CREATED);
    }

    /**
     * Deletes the key with the specified Hashid.
     *
     * <p>This endpoint is mapped to "{@value RequestMappings#KEYS}{@value RequestMappings#KEY_HASHID}".
     *
     * @param keyHashid the Hashid of the key to delete
     * @return a empty response entity
     * @since 0.2.0
     */
    @DeleteMapping(value = KEY_HASHID)
    @Secured(AuthoritiesConstants.ADMIN)
    public ResponseEntity<Void> delete(@PathVariable(name = KEY_ID) String keyHashid) {
        keyService.delete(decodeSingleValueHashid(keyHashid));
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    /**
     * Deletes all keys.
     *
     * <p>This endpoint is mapped to "{@value RequestMappings#KEYS}".
     *
     * @return a empty response entity
     * @since 0.2.0
     */
    @DeleteMapping
    @Secured(AuthoritiesConstants.ADMIN)
    public ResponseEntity<Void> deleteAll() {
        keyService.deleteAll();
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    /**
     * Gets the key with the specified Hashid.
     *
     * <p>This endpoint is mapped to "{@value RequestMappings#KEYS}{@value RequestMappings#KEY_HASHID}".
     *
     * @param keyHashid the Hashid of the key entity to get
     * @return the key as response entity
     */
    @GetMapping(value = KEY_HASHID)
    @Secured(AuthoritiesConstants.USER)
    public ResponseEntity get(@PathVariable(name = KEY_ID) String keyHashid) {
        return keyService.get(decodeSingleValueHashid(keyHashid))
                .map(key -> new ResponseEntity<>(key, HttpStatus.OK))
                .orElseGet(() -> new ResponseEntity<>(HttpStatus.FORBIDDEN));
    }

    /**
     * Gets the subscribers of a key with the specified Hashid.
     *
     * <p>This endpoint is mapped to "{@value RequestMappings#KEYS}{@value RequestMappings#KEY_SUBSCRIBERS}".
     *
     * @param keyHashid the Hashid of the key entity to get
     * @return the key as response entity
     */
    @GetMapping(value = KEY_SUBSCRIBERS)
    @Secured(AuthoritiesConstants.USER)
    public ResponseEntity getSubscribers(@PathVariable(name = KEY_ID) String keyHashid) {
        return keyService.getSubscribers(decodeSingleValueHashid(keyHashid))
            .map(subscribers -> new ResponseEntity<>(subscribers, HttpStatus.OK))
            .orElseGet(() -> new ResponseEntity<>(HttpStatus.FORBIDDEN));
    }

    /**
     * Gets the userEncryptedPassword for the specified Hashid
     *
     * <p>This endpoint is mapped to "{@value RequestMappings#KEYS}{@value RequestMappings#KEY_ENCRYPTED_PASSWORD}".
     *
     * @param keyHashid the Hashid of the key entity to get
     * @return the userEncryptedPassword of the requesting user for the specified key as response entity
     * @throws KeyNotFoundException if the Hashid is invalid and the key has not been found
     * @throws UserEncryptedPasswordNotFoundException if the Hashid is invalid and the key has not been found
     */
    @GetMapping(value = KEY_ENCRYPTED_PASSWORD)
    @Secured(AuthoritiesConstants.USER)
    public ResponseEntity getEncryptedPassword(@PathVariable(name = KEY_ID) String keyHashid) {
        return keyService.getUserEncryptedPassword(decodeSingleValueHashid(keyHashid))
            .map(userEncryptedPassword -> new ResponseEntity<>(userEncryptedPassword, HttpStatus.OK))
            .orElseGet(() -> new ResponseEntity<>(HttpStatus.FORBIDDEN));
    }

    /**
     * Gets all keys.
     *
     * <p>This endpoint is mapped to "{@value RequestMappings#KEYS}".
     *
     * @return a collection of keys as response entity
     */
    @GetMapping
    @Secured(AuthoritiesConstants.USER)
    public ResponseEntity<Set<Key>> getAll() {
        return new ResponseEntity<>(keyService.getAll(), HttpStatus.OK);
    }

    /**
     * Updates a key with the specified payload.
     *
     * <p>This endpoint is mapped to "{@value RequestMappings#KEYS}{@value RequestMappings#KEY_HASHID}".
     *
     * @param keyHashid the Hashid of the key entity to update
     * @param payload the key request payload to update the key entity with
     * @return the updated key as response entity
     * @since 0.2.0
     */
    @PutMapping(value = KEY_HASHID)
    @Secured(AuthoritiesConstants.ADMIN)
    public ResponseEntity<Key> update(@PathVariable(name = KEY_ID) String keyHashid, @Valid @RequestBody KeyPayload payload) {
        return new ResponseEntity<>(keyService.update(decodeSingleValueHashid(keyHashid), payload), HttpStatus.OK);
    }

    /**
     * Decodes the specified Hashid.
     *
     * @param hashid the Hashid of the key to decode
     * @return the decoded Hashid if valid
     * @throws KeyNotFoundException if the Hashid is invalid and the key has not been found
     * @since 0.12.0
     */
    private Long decodeSingleValueHashid(String hashid) throws HashidNotValidException {
        long[] decodedHashid = hashids.decode(hashid);
        if (decodedHashid.length == 0) {
            throw new HashidNotValidException();
        }
        return decodedHashid[0];
    }
}
