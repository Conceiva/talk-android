/*
 * Nextcloud Talk application
 *
 * @author Mario Danic
 * Copyright (C) 2017 Mario Danic <mario@lovelyhq.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.nextcloud.talk.models.json.push;

import com.bluelinelabs.logansquare.annotation.JsonField;
import com.bluelinelabs.logansquare.annotation.JsonIgnore;
import com.bluelinelabs.logansquare.annotation.JsonObject;
import lombok.Data;
import org.parceler.Parcel;

@Data
@Parcel
@JsonObject
public class DecryptedPushMessage {
    @JsonField(name = "app")
    String app;

    @JsonField(name = "type")
    String type;

    @JsonField(name = "subject")
    String subject;

    @JsonField(name = "id")
    String id;

    @JsonField(name = "nid")
    long notificationId;

    @JsonField(name = "delete")
    boolean delete;

    @JsonField(name = "delete-all")
    boolean deleteAll;

    @JsonIgnore
    NotificationUser notificationUser;

    @JsonIgnore
    String text;

    @JsonIgnore
    long timestamp;
}
