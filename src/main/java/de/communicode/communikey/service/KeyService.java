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
package de.communicode.communikey.service;

import static de.communicode.communikey.controller.RequestMappings.QUEUE_UPDATES_KEYS;
import static de.communicode.communikey.controller.RequestMappings.QUEUE_UPDATES_KEYS_DELETE;
import static de.communicode.communikey.security.AuthoritiesConstants.ADMIN;
import static de.communicode.communikey.security.SecurityUtils.getCurrentUserLogin;
import static de.communicode.communikey.security.SecurityUtils.isCurrentUserInRole;
import static java.util.Objects.requireNonNull;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.toSet;

import de.communicode.communikey.domain.Key;
import de.communicode.communikey.domain.KeyCategory;
import de.communicode.communikey.domain.User;
import de.communicode.communikey.domain.UserGroup;
import de.communicode.communikey.domain.UserEncryptedPassword;
import de.communicode.communikey.exception.HashidNotValidException;
import de.communicode.communikey.exception.KeyNotAccessibleByUserException;
import de.communicode.communikey.exception.KeyNotFoundException;
import de.communicode.communikey.exception.UserEncryptedPasswordNotFoundException;
import de.communicode.communikey.repository.EncryptionJobRepository;
import de.communicode.communikey.repository.UserEncryptedPasswordRepository;
import de.communicode.communikey.security.AuthoritiesConstants;
import de.communicode.communikey.security.SecurityUtils;
import de.communicode.communikey.service.payload.KeyPayload;
import de.communicode.communikey.repository.KeyRepository;
import de.communicode.communikey.repository.UserRepository;
import de.communicode.communikey.service.payload.KeyPayloadEncryptedPasswords;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hashids.Hashids;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

/**
 * The REST API service to process {@link Key} entities via a {@link KeyRepository}.
 *
 * @author sgreb@communicode.de
 * @author dvonderbey@communicode.de
 * @since 0.1.0
 */
@Service
public class KeyService {

    private static final Logger log = LogManager.getLogger();
    private final KeyRepository keyRepository;
    private final UserEncryptedPasswordRepository userEncryptedPasswordRepository;
    private final KeyCategoryService keyCategoryService;
    private final UserService userService;
    private final Hashids hashids;
    private final UserRepository userRepository;
    private final AuthorityService authorityService;
    private final EncryptionJobService encryptionJobService;
    private final EncryptionJobRepository encryptionJobRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Autowired
    public KeyService(KeyRepository keyRepository, @Lazy KeyCategoryService keyCategoryService,
                      UserService userRestService, Hashids hashids, UserEncryptedPasswordRepository
                      userEncryptedPasswordRepository, UserRepository userRepository,
                      AuthorityService authorityService, @Lazy EncryptionJobService encryptionJobService,
                      EncryptionJobRepository encryptionJobRepository,
                      SimpMessagingTemplate messagingTemplate) {
        this.keyRepository = requireNonNull(keyRepository, "keyRepository must not be null!");
        this.userEncryptedPasswordRepository = requireNonNull(userEncryptedPasswordRepository, "userEncryptedPasswordRepository must not be null!");
        this.keyCategoryService = requireNonNull(keyCategoryService, "keyCategoryService must not be null!");
        this.userService = requireNonNull(userRestService, "userService must not be null!");
        this.hashids = requireNonNull(hashids, "hashids must not be null!");
        this.userRepository = requireNonNull(userRepository, "userRepository must not be null!");
        this.authorityService = requireNonNull(authorityService, "authorityService must not be null!");
        this.encryptionJobService = requireNonNull(encryptionJobService, "encryptionJobService must not be null!");
        this.encryptionJobRepository = requireNonNull(encryptionJobRepository, "encryptionJobRepository must not be null!");
        this.messagingTemplate = requireNonNull(messagingTemplate, "messagingTemplate must not be null!");
    }

    /**
     * Creates a new key.
     *
     * @param payload the key payload
     * @return the created key
     */
    public Key create(KeyPayload payload) {
        Key key = new Key();
        checkPayloadKeyAccess(key, payload);
        String userLogin = getCurrentUserLogin();
        User user = userService.validate(userLogin);
        key.setCreator(user);
        key.setName(payload.getName());
        key.setLogin(payload.getLogin());
        key.setNotes(payload.getNotes());
        Key persistedKey = keyRepository.save(key);
        persistedKey.setHashid(hashids.encode(persistedKey.getId()));
        persistedKey = keyRepository.save(persistedKey);
        log.debug("Created new key with ID '{}'", persistedKey.getId());
        for (KeyPayloadEncryptedPasswords encryptedPasswordsPayload : payload.getEncryptedPasswords()) {
            createUserEncryptedPasswords(key, encryptedPasswordsPayload);
        }
        if (ofNullable(payload.getCategoryId()).isPresent()) {
            keyCategoryService.addKey(decodeSingleValueHashid(payload.getCategoryId()), persistedKey.getId());
            persistedKey = keyRepository.findById(persistedKey.getId()).orElseThrow(KeyNotFoundException::new);
        }
        userService.addKey(userLogin, persistedKey);
        sendUpdates(key);
        return persistedKey;
    }

    /**
     * Creates new userEncryptedPassword from KeyPayloadEncryptedPasswords.
     *
     * @param payload the array of userEncryptedPasswords from a key payload
     */
    private void createUserEncryptedPasswords(Key key, KeyPayloadEncryptedPasswords payload) {
        UserEncryptedPassword newUserEncryptedPassword = new UserEncryptedPassword();
        newUserEncryptedPassword.setKey(key);
        newUserEncryptedPassword.setOwner(userService.validate(payload.getLogin()));
        newUserEncryptedPassword.setPassword(payload.getEncryptedPassword());
        userEncryptedPasswordRepository.save(newUserEncryptedPassword);
        key.addUserEncryptedPassword(newUserEncryptedPassword);
        keyRepository.save(key);
        userService.addUserEncryptedPassword(payload.getLogin(), newUserEncryptedPassword);
        log.debug("Created userencryptedPassword for key {} and user {}", key.getId(), payload.getLogin());
    }

    /**
     * Deletes the key with the specified ID.
     *
     * @param keyId the ID of the key to delete
     * @throws KeyNotFoundException if the key with the specified ID has not been found
     */
    public void delete(Long keyId) {
        Key key = validate(keyId);
        userEncryptedPasswordRepository.deleteByKey(key);
        encryptionJobRepository.deleteByKey(key);
        keyRepository.delete(key);
        sendRemovalUpdates(key);
        log.debug("Deleted key with ID '{}'", keyId);
    }

    /**
     * Deletes all keys.
     */
    public void deleteAll() {
        userEncryptedPasswordRepository.deleteAll();
        keyRepository.deleteAll();
        log.debug("Deleted all keys");
    }

    /**
     * Gets the key with the specified ID if the current user is authorized to receive.
     *
     * <p> The returned key is based on the linked {@link KeyCategory} which is filtered by the {@link UserGroup} the user is assigned to.
     *
     * @param keyId the ID of the key to get
     * @return the key, {@link Optional#empty()} otherwise
     * @throws KeyNotFoundException if the key with the specified ID has not been found
     */
    public Optional<Key> get(Long keyId) {
        if (isCurrentUserInRole(ADMIN)) {
            return Optional.ofNullable(validate(keyId));
        }

        Key key = validate(keyId);
        if (userService.validate(getCurrentUserLogin()).getGroups().stream()
            .anyMatch(userGroup -> ofNullable(key.getCategory()).map(cat -> cat.getGroups().contains(userGroup)).orElse(false))) {
            return Optional.of(key);
        }
        return Optional.empty();
    }

    /**
     * Gets all keys the current user is authorized to receive.
     *
     * <p> The returned keys are based on their linked {@link KeyCategory} which are filtered by the {@link UserGroup} the user is assigned to.
     *
     * @return a collection of keys
     */
    public Set<Key> getAll() {
        if (isCurrentUserInRole(ADMIN)) {
            return new HashSet<>(keyRepository.findAll());
        }
        return userService.validate(getCurrentUserLogin()).getGroups().stream()
            .flatMap(userGroup -> userGroup.getCategories().stream())
            .flatMap(keyCategory -> keyCategory.getKeys().stream())
            .collect(toSet());
    }

    /**
     * Gets a userEncryptedPassword for the specified hashid
     *
     * @param hashid the hashid of the key
     * @return a userEncryptedPassword
     */
    public Optional<UserEncryptedPassword> getUserEncryptedPassword(Long hashid) {
        String login = SecurityUtils.getCurrentUserLogin();
        User user = userService.validate(login);
        Key key = validate(hashid);
        UserEncryptedPassword userEncryptedPassword = null;
        if(user.getAuthorities().contains(authorityService.get(ADMIN))) {
            userEncryptedPassword = userEncryptedPasswordRepository.findOneByOwnerAndKey(user, key);
        } else {
            for (UserGroup userGroup:key.getCategory().getGroups()) {
                if(user.getGroups().contains(userGroup)) {
                    userEncryptedPassword = userEncryptedPasswordRepository.findOneByOwnerAndKey(user, key);
                }
            }
        }
        ofNullable(userEncryptedPassword)
            .orElseThrow(UserEncryptedPasswordNotFoundException::new);
        return Optional.of(userEncryptedPassword);
    }

    /**
     * Updates a key with the specified payload.
     *
     * @param keyId the ID of the key to update
     * @param payload the payload to update the key with
     * @return the updated key
     * @since 0.2.0
     */
    public Key update(Long keyId, KeyPayload payload) {
        Key key = validate(keyId);
        checkPayloadKeyAccess(key, payload);
        key.setLogin(payload.getLogin());
        key.setName(payload.getName());
        key.setLogin(payload.getLogin());
        key.setNotes(payload.getNotes());
        key.getUserEncryptedPasswords()
            .forEach(userEncryptedPassword -> {
                userEncryptedPassword.getOwner().removeUserEncryptedPassword(userEncryptedPassword);
                userRepository.save(userEncryptedPassword.getOwner());
                log.debug("Removed userEncryptedPassword with id '{}' for user '{}'",
                            userEncryptedPassword.getId(),
                            userEncryptedPassword.getOwner().getId());
            });
        key.removeAllUserEncryptedPasswords();
        key = keyRepository.save(key);
        userEncryptedPasswordRepository.deleteByKey(key);
        for (KeyPayloadEncryptedPasswords encryptedPasswordsPayload : payload.getEncryptedPasswords()) {
            UserEncryptedPassword userEncryptedPassword = userEncryptedPasswordRepository.findOneByOwnerAndKey(
                userService.validate(encryptedPasswordsPayload.getLogin()), validate(keyId));
            if(userEncryptedPassword != null) {
                log.debug("HTTP- Updating old userEncryptedPassword");
                userEncryptedPassword.setPassword(encryptedPasswordsPayload.getEncryptedPassword());
                userEncryptedPasswordRepository.save(userEncryptedPassword);
            } else {
                log.debug("HTTP- Creating new userEncryptedPassword");
                createUserEncryptedPasswords(key, encryptedPasswordsPayload);
            }
        }
        final Key savedKey = keyRepository.save(key);
        encryptionJobService.createForKey(savedKey);

        sendUpdates(key);
        return key;
    }

    /**
     * Removes keys of a user that are obsolete because their visibility to the user changed
     *
     * @param user the user user to update
     * @author dvonderbey@communicode.de
     * @since 0.15.0
     */
    public void removeObsoletePasswords(User user) {
        if (user.getAuthorities().stream()
            .noneMatch(authority -> authority.getName().equals(ADMIN))) {
            userEncryptedPasswordRepository.findAllByOwner(user)
                .forEach(userEncryptedPassword -> {
                    Key key = userEncryptedPassword.getKey();
                    KeyCategory category = key.getCategory();
                    if (category != null) {
                        Set<UserGroup> userGroups = category.getGroups();
                        if (userGroups.isEmpty()) {
                            deleteUserEncryptedPassword(key, userEncryptedPassword);
                        } else {
                            userGroups.forEach(userGroup -> {
                                if (!user.getGroups().contains(userGroup)) {
                                    deleteUserEncryptedPassword(key, userEncryptedPassword);
                                }
                            });
                        }

                    } else if (!key.getCreator().equals(user)) {
                        deleteUserEncryptedPassword(key, userEncryptedPassword);
                    }
                });
        }
    }

    /**
     * Checks if the intended owner of the userEncryptedPassword has access to the key itself.
     * Returns true, if the user who intents to PUT a new encrypted Password in the Set has indeed
     * access to it. Otherwise returns false
     *
     * @author lleifermann@communicode.de
     * @param key the key
     * @param payload the payload to inspect
     * @return boolean
     * @since 0.15.0
     */
    private boolean checkKeyAccess(Key key, KeyPayloadEncryptedPasswords payload) {
        User user = userService.validate(payload.getLogin());
        if(user.getAuthorities().contains(authorityService.get(ADMIN))) {
            return true;
        }
        KeyCategory keyCategory = key.getCategory();
        for (UserGroup userGroup:keyCategory.getGroups()) {
            if(user.getGroups().contains(userGroup)) {
                return true;
            }
        }
        log.info("User '{}' tried to add an encryptedPassword for user {} without access to the key.", getCurrentUserLogin(), user.getLogin());
        return false;
    }

    /**
     * Removes all user encrypted passwords of a user.
     *
     * @param user the user whose passwords should be deleted
     * @author dvonderbey@communicode.de
     * @since 0.15.0
     */
    public void removeAllUserEncryptedPasswordsForUser(User user) {
        userEncryptedPasswordRepository.findAllByOwner(user)
            .forEach(userEncryptedPassword -> {
                user.removeUserEncryptedPassword(userEncryptedPassword);
                userRepository.save(user);
                userEncryptedPassword.getKey().removeUserEncryptedPassword(userEncryptedPassword);
                userEncryptedPasswordRepository.save(userEncryptedPassword);
                userEncryptedPasswordRepository.delete(userEncryptedPassword);
            });
        log.debug("Removed all encrypted passwords for user '{}'", user.getLogin());
    }

    /**
     * Check encryptedPasswords in a given payload for key access.
     *
     * @author lleifermann@communicode.de
     * @param key the key
     * @param keyPayload the payload to inspect
     * @since 0.15.0
     */
    private void checkPayloadKeyAccess(Key key, KeyPayload keyPayload) {
        for (KeyPayloadEncryptedPasswords encryptedPasswordsPayload : keyPayload.getEncryptedPasswords()) {
            if (!checkKeyAccess(key, encryptedPasswordsPayload)) {
                throw new KeyNotAccessibleByUserException();
            }
        }
    }

    /**
     * Returns a list of public keys and the names of the subscribers for a specific key
     * Goes through the category of the key to find all groups and their members that
     * are able to "see" the key. Also adds all admins to the list.
     *
     * @param keyId the keyId of the key of which the subscribers are wanted
     * @return An optional containing a collection of user subscriber info
     * @author dvonderbey@communicode.de
     * @since 0.15.0
     */
    public Optional<Set<User.SubscriberInfo>> getSubscribers(Long keyId) {
        return getAccessors(validate(keyId)).stream()
            .filter(user -> user.getPublicKey() != null)
            .map(User::getSubscriberInfo)
            .collect(collectingAndThen(toSet(), Optional::of));
    }

    /**
     * Returns a set of Users that should have access to a key.
     *
     * @param key the key of which the accessors are wanted
     * @return A collection of users
     * @author dvonderbey@communicode.de
     * @since 0.15.0
     */
    public Set<User> getAccessors(Key key) {
        Stream<User> subscriberStream = Optional.of(key)
            .map(Key::getCategory)
            .map(KeyCategory::getGroups)
            .map(Collection::stream)
            .orElse(Stream.empty())
            .flatMap(userGroup -> userGroup.getUsers().stream());

        Stream<User> adminStream = userRepository.findAllByAuthorities(authorityService.get(AuthoritiesConstants.ADMIN))
            .stream();

        return Stream.concat(subscriberStream, adminStream)
            .collect(toSet());
    }

    /**
     * Deletes an userEncryptedPassword from a key and from the repository
     *
     * @param key the key of the userEncryptedPassword
     * @param userEncryptedPassword the userEncryptedPassword to delete
     * @author dvonderbey@communicode.de
     * @since 0.15.0
     */
    private void deleteUserEncryptedPassword(Key key, UserEncryptedPassword userEncryptedPassword) {
        User owner = userEncryptedPassword.getOwner();
        owner.removeUserEncryptedPassword(userEncryptedPassword);
        userRepository.save(owner);
        key.removeUserEncryptedPassword(userEncryptedPassword);
        keyRepository.save(key);
        userEncryptedPasswordRepository.delete(userEncryptedPassword);
        log.debug("Removed encryptedPassword '{}’ of key '{}' of user {}.",
            userEncryptedPassword.getId(),
            key.getId(),
            owner.getId());
    }

    /**
     * Returns a list of users that own a UserEncryptedPassword of the specified key.
     *
     * @param key the key the user should have an userEncryptedPassword of
     * @return a collection of user subscriber info
     * @author dvonderbey@communicode.de
     * @since 0.15.0
     */
    public Set<User.SubscriberInfo> getQualifiedEncoders(Key key) {
        return userEncryptedPasswordRepository.findAllByKey(key).stream()
            .map(UserEncryptedPassword::getOwner)
            .map(User::getSubscriberInfo)
            .collect(toSet());
    }

    /**
     * Validates a key with the specified ID.
     *
     * @param keyId the ID of the key to validate
     * @return the key if validated
     * @throws KeyNotFoundException if the key with the specified ID has not been found
     */
    public Key validate(Long keyId) {
        return keyRepository.findById(keyId).orElseThrow(KeyNotFoundException::new);
    }

    /**
     * Decodes the specified Hashid.
     *
     * @param hashid the Hashid of the key to decode
     * @return the decoded Hashid if valid
     * @throws KeyNotFoundException if the Hashid is invalid and the key has not been found
     * @since 0.13.0
     */
    private Long decodeSingleValueHashid(String hashid) {
        long[] decodedHashid = hashids.decode(hashid);
        if (decodedHashid.length == 0) {
            throw new HashidNotValidException();
        }
        return decodedHashid[0];
    }

    /**
     * Sends out websocket messages to users for live updates.
     *
     * @param key the key that was updated
     * @author dvonderbey@communicode.de
     * @since 0.15.0
     */
    public void sendUpdates(Key key) {
        getAccessors(key)
            .forEach(user -> messagingTemplate.convertAndSendToUser(user.getLogin(), QUEUE_UPDATES_KEYS, key));
        log.debug("Sent out updates for key '{}'.", key.getId());
    }

    /**
     * Sends out websocket messages to users for live removals.
     *
     * @param key the key that was removed
     * @author dvonderbey@communicode.de
     * @since 0.15.0
     */
    public void sendRemovalUpdates(Key key) {
        getAccessors(key)
            .forEach(user -> messagingTemplate.convertAndSendToUser(user.getLogin(), QUEUE_UPDATES_KEYS_DELETE, key));
        log.debug("Sent out removal updates for key '{}'.", key.getId());
    }
}
