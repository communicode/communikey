/*
 * Copyright (C) communicode AG - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * 2016
 */
package de.communicode.communikey.controller;

import static de.communicode.communikey.config.CommunikeyConstants.ENDPOINT_USERS;
import static de.communicode.communikey.config.CommunikeyConstants.REQUEST_VARIABLE_USER_ID;
import static java.util.Objects.requireNonNull;

import de.communicode.communikey.config.CommunikeyConstants;
import de.communicode.communikey.domain.User;
import de.communicode.communikey.domain.UserDto;
import de.communicode.communikey.domain.converter.UserDtoConverter;
import de.communicode.communikey.exception.UserNotFoundException;
import de.communicode.communikey.service.UserRestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * The REST API controller to process {@link User} entities.
 * <p>
 *     Mapped to the {@value CommunikeyConstants#ENDPOINT_USERS} endpoint.
 *
 * @author sgreb@communicode.de
 * @since 0.2.0
 */
@RestController
@RequestMapping(ENDPOINT_USERS)
public class UserController {

    private final UserRestService userService;

    private final UserDtoConverter userConverter;

    @Autowired
    public UserController(UserRestService userService, UserDtoConverter userConverter) {
        this.userService = requireNonNull(userService, "userService must not be null!");
        this.userConverter = requireNonNull(userConverter, "userConverter must not be null!");
    }

    /**
     * Gets all {@link User} entities.
     * <p>
     *     This endpoint is mapped to "{@value CommunikeyConstants#ENDPOINT_USERS}{@value CommunikeyConstants#REQUEST_VARIABLE_USER_ID}".
     *
     * @param limit the amount of user data transfer objects to include in the response
     * @param username the name of the user entities to get
     * @return a collection of user data transfer objects
     */
    @GetMapping
    Set<UserDto> getAll(@RequestParam(required = false) Long limit,
                        @RequestParam(name = "username", required = false) String username) {
        return userService.getAll().stream()
            .filter(user -> username == null || username.equalsIgnoreCase(user.getUsername()))
            .limit(Optional.ofNullable(limit).orElse(Long.MAX_VALUE))
            .map(userConverter)
            .collect(Collectors.toSet());
    }

    /**
     * Gets the {@link User} entity with the specified ID.
     * <p>
     *     This endpoint is mapped to "{@value CommunikeyConstants#ENDPOINT_USERS}{@value CommunikeyConstants#REQUEST_VARIABLE_USER_ID}".
     *
     * @param userId the ID of the user entity to get
     * @return the user data transfer object
     * @throws UserNotFoundException if the user entity with the specified ID has not been found
     */
    @GetMapping(value = REQUEST_VARIABLE_USER_ID)
    UserDto get(@PathVariable long userId) throws UserNotFoundException {
        return convertToDto(userService.getById(userId));
    }

    /**
     * Converts a user entity to the associated user data transfer object.
     *
     * @param user the user entity to convert
     * @return the converted user data transfer object
     */
    private UserDto convertToDto(User user) {
        return userConverter.convert(user);
    }

    /**
     * Converts a user data transfer object to the associated user entity.
     *
     * @param userDto the user data transfer object to convert
     * @return the converted user entity
     * @throws UserNotFoundException if the associated user entity of the user data transfer object has not been found
     */
    private User convertToEntity(UserDto userDto) throws UserNotFoundException {
        return userService.getById(userDto.getId());
    }
}