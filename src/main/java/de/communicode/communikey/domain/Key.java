/*
 * Copyright (C) communicode AG - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * 2017
 */
package de.communicode.communikey.domain;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonIdentityReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import com.google.common.collect.Sets;
import org.hibernate.validator.constraints.NotBlank;

import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.FetchType;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Id;
import javax.persistence.GeneratedValue;
import javax.persistence.Column;
import javax.persistence.GenerationType;
import javax.validation.constraints.NotNull;

import java.io.Serializable;
import java.util.Set;

/**
 * Represents a key.
 *
 * @author sgreb@communicode.de
 * @since 0.1.0
 */
@Entity
@Table(name = "\"keys\"")
public class Key extends AbstractEntity implements Serializable {

    private static final long serialVersionUID = 1;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonIgnore
    private Long id;

    @Column
    @JsonProperty("id")
    private String hashid;

    @NotBlank
    @Column(nullable = false)
    private String name;

    @ManyToOne
    @JoinColumn(name = "key_category_id")
    @JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
    @JsonIdentityReference(alwaysAsId = true)
    private KeyCategory category;

    @ManyToOne
    @JoinColumn(name = "creator_user_id", nullable = false)
    @JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
    @JsonIdentityReference(alwaysAsId = true)
    private User creator;

    @NotNull
    @Column(nullable = false)
    private String login;

    @JsonIgnore
    @ManyToMany(fetch = FetchType.LAZY, mappedBy = "key")
    @JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
    @JsonIdentityReference(alwaysAsId = true)
    private Set<UserEncryptedPassword> userEncryptedPasswords = Sets.newConcurrentHashSet();

    @ManyToMany(fetch = FetchType.LAZY, mappedBy = "keys")
    @JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
    @JsonIdentityReference(alwaysAsId = true)
    private Set<Tag> tags = Sets.newConcurrentHashSet();

    @Lob
    @Column(length = 500)
    private String notes;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    /**
     * @return the Hashid
     * @since 0.12.0
     */
    public String getHashid() {
        return hashid;
    }

    /**
     * @param hashid the Hashid
     * @since 0.12.0
     */
    public void setHashid(String hashid) {
        this.hashid = hashid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public KeyCategory getCategory() {
        return category;
    }

    public void setCategory(KeyCategory category) {
        this.category = category;
    }

    public User getCreator() {
        return creator;
    }

    public void setCreator(User creator) {
        this.creator = creator;
    }

    public String getLogin() {
        return login;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public Set<UserEncryptedPassword> getUserEncryptedPasswords() {
        return Sets.newConcurrentHashSet(userEncryptedPasswords);
    }

    public void setUserEncryptedPasswords(Set<UserEncryptedPassword> userEncryptedPasswords) {
        this.userEncryptedPasswords = userEncryptedPasswords;
    }

    public boolean addUserEncryptedPassword(UserEncryptedPassword userEncryptedPassword) {
        return userEncryptedPasswords.add(userEncryptedPassword);
    }

    public boolean addUserEncryptedPasswords(Set<UserEncryptedPassword> userEncryptedPassword) {
        return this.userEncryptedPasswords.addAll(userEncryptedPassword);
    }

    public boolean removeUserEncryptedPassword(UserEncryptedPassword userEncryptedPassword) {
        return userEncryptedPasswords.remove(userEncryptedPassword);
    }

    public boolean removeUserEncryptedPasswords(Set<UserEncryptedPassword> userEncryptedPassword) {
        return this.userEncryptedPasswords.removeAll(userEncryptedPassword);
    }

    public void removeAllUserEncryptedPasswords() {
        userEncryptedPasswords.clear();
    }

    public boolean addTag(Tag tag) {
        return this.tags.add(tag);
    }

    public boolean addTags(Set<Tag> tags) {
        return this.tags.addAll(tags);
    }

    public boolean removeTag(Tag tag) {
        return this.tags.remove(tag);
    }

    public boolean removeTags(Set<Tag> tags) {
        return this.tags.removeAll(tags);
    }

    public void removeAllTags() {
        this.tags.clear();
    }

    public Set<Tag> getTags() {
        return Sets.newConcurrentHashSet(this.tags);
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    @Override
    public String toString() {
        return "Key{" +
            "id=" + id +
            ", name='" + name + '\'' +
            ", category=" + category +
            ", creator=" + creator +
            '}';
    }
}
