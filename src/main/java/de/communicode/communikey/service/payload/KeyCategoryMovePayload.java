/*
 * Copyright (C) communicode AG - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * 2017
 */
package de.communicode.communikey.service.payload;

import de.communicode.communikey.domain.KeyCategory;

/**
 * A payload object for a {@link KeyCategory} move action.
 *
 * @author lleifermann@communicode.de
 * @since 0.17.0
 */
public class KeyCategoryMovePayload {

    private String parent;

    public KeyCategoryMovePayload() {}

    public String getParent() { return parent; }

    @Override
    public String toString() {
        return "KeyCategoryPayload{" + "parent='" + parent + '\'' + '}';
    }
}