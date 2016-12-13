/*
 * Copyright (C) communicode AG - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * 2016
 */
package de.communicode.communikey.domain;

import static de.communicode.communikey.config.DataSourceConfig.KEYS;
import static de.communicode.communikey.config.DataSourceConfig.CREATOR_USER_ID;
import static de.communicode.communikey.config.DataSourceConfig.KEY_CATEGORY_ID;
import static de.communicode.communikey.config.DataSourceConfig.KEY_ID;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonManagedReference;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GenerationType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import java.sql.Timestamp;
import java.util.Calendar;

/**
 * Represents a key entity.
 *
 * @author sgreb@communicode.de
 * @since 0.1.0
 */
@Entity
@Table(name = KEYS)
public class Key {
    @Id
    @Column(name = KEY_ID, nullable = false)
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long id;

    @ManyToOne
    @JoinColumn(name = CREATOR_USER_ID, nullable = false)
    @JsonManagedReference
    @JsonIgnoreProperties(value = {"roles", "groups", "credentialsNonExpired", "accountNonExpired", "accountNonLocked", "enabled"})
    private User creator;

    @ManyToOne
    @JoinColumn(name = KEY_CATEGORY_ID)
    @JsonManagedReference
    @JsonIgnoreProperties(value = {"creator", "responsible", "parent", "groups"})
    private KeyCategory category;

    private Timestamp creationTimestamp;

    private String value;

    private Key() {}

    /**
     * Constructs a new key entity with the given attributes, an auto-generated ID and creation timestamp.
     *
     * @param value the value of the key
     * @param creator the user who created this key
     */
    public Key(String value, User creator) {
        this.creator = creator;
        this.value = value;
        this.creationTimestamp = new Timestamp(Calendar.getInstance().getTimeInMillis());
    }

    public KeyCategory getCategory() {
        return this.category;
    }

    public Timestamp getCreationTimestamp() {
        return creationTimestamp;
    }

    public User getCreator() {
        return creator;
    }

    public long getId() {
        return id;
    }

    public String getValue() {
        return value;
    }

    public void setCategory(KeyCategory category) {
        this.category = category;
    }

    public void setValue(String value) {
        this.value = value;
    }
}