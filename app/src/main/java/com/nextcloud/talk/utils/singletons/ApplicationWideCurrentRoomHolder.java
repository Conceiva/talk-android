/*
 * Nextcloud Talk application
 *
 * @author Mario Danic
 * Copyright (C) 2017-2018 Mario Danic <mario@lovelyhq.com>
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

package com.nextcloud.talk.utils.singletons;

import com.nextcloud.talk.models.database.UserEntity;
import com.nextcloud.talk.models.json.participants.Participant;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ApplicationWideCurrentRoomHolder {
    private static final ApplicationWideCurrentRoomHolder holder = new ApplicationWideCurrentRoomHolder();
    private String currentRoomId = "";
    private String currentRoomToken = "";
    private UserEntity userInRoom = new UserEntity();

    public List<HashMap<String, Object>> getParticipantsList() {
        return participantsList;
    }

    public void setParticipantsList(List<HashMap<String, Object>> participantsList) {
        this.participantsList = participantsList;
    }

    List<HashMap<String, Object>> participantsList=new ArrayList<>();
//    private <L userInRoom = new UserEntity();
    private boolean inCall = false;
    private String session = "";

    public static ApplicationWideCurrentRoomHolder getInstance() {
        return holder;
    }

    public void clear() {
        currentRoomId = "";
        userInRoom = new UserEntity();
        inCall = false;
        currentRoomToken = "";
        session = "";
        participantsList.clear();
    }

    public String getCurrentRoomToken() {
        return currentRoomToken;
    }

    public void setCurrentRoomToken(String currentRoomToken) {
        this.currentRoomToken = currentRoomToken;
    }

    public String getCurrentRoomId() {
        return currentRoomId;
    }

    public void setCurrentRoomId(String currentRoomId) {
        this.currentRoomId = currentRoomId;
    }

    public UserEntity getUserInRoom() {
        return userInRoom;
    }

    public void setUserInRoom(UserEntity userInRoom) {
        this.userInRoom = userInRoom;
    }

    public boolean isInCall() {
        return inCall;
    }

    public void setInCall(boolean inCall) {
        this.inCall = inCall;
    }

    public String getSession() {
        return session;
    }

    public void setSession(String session) {
        this.session = session;
    }

    public HashMap<String,Object> getParticipantForSessionID(@NotNull String sessionId) {
        HashMap<String,Object> selectedparticipant=null;
        for(HashMap<String,Object> participant:  participantsList )
        {
            if(participant.get("sessionId").toString().equalsIgnoreCase(sessionId))
            selectedparticipant=participant;
        }
        return selectedparticipant;
    }
}
