package com.nextcloud.talk.models.json.meetings;

import android.os.Parcelable;

import java.util.List;
import javax.annotation.Generated;

import com.bluelinelabs.logansquare.annotation.JsonField;
import com.bluelinelabs.logansquare.annotation.JsonObject;
import com.nextcloud.talk.models.database.User;

import org.parceler.Parcel;

import io.requery.Persistable;
import lombok.Data;

@Parcel
@Data
@JsonObject
public class MeetingsReponse implements User, Persistable, Parcelable {

	@JsonField(name ="owner")
	private boolean owner;

	@JsonField(name ="recurring")
	private boolean recurring;

	@JsonField(name ="timezone")
	private String timezone;

	@JsonField(name ="attendees")
	private String attendees;

	@JsonField(name ="start")
	private int start;


	@JsonField(name ="active")
	private boolean active;


	@JsonField(name ="description")
	private String description;


	@JsonField(name ="meetingid")
	private int meetingid;


	@JsonField(name ="title")
	private String title;


	/*@JsonField(name ="uuid")
	private List<String> uuid;

*/
	@JsonField(name ="uri")
	private String uri;


	@JsonField(name ="found")
	private boolean found;


	@JsonField(name ="public")
	private boolean jsonMemberPublic;


	@JsonField(name ="calendaruri")
	private String calendaruri;


	@JsonField(name ="serverUrl")
	private String serverUrl;


	@JsonField(name ="modpin")
	private String modpin;


	@JsonField(name ="end")
	private int end;


	@JsonField(name ="state")
	private int state;


	@JsonField(name ="vcalendar")
	private String vcalendar;

	public void setOwner(boolean owner){
		this.owner = owner;
	}

	public boolean isOwner(){
		return owner;
	}

	public void setRecurring(boolean recurring){
		this.recurring = recurring;
	}

	public boolean isRecurring(){
		return recurring;
	}

	public void setTimezone(String timezone){
		this.timezone = timezone;
	}

	public String getTimezone(){
		return timezone;
	}

	public void setAttendees(String attendees){
		this.attendees = attendees;
	}

	public String getAttendees(){
		return attendees;
	}

	public void setStart(int start){
		this.start = start;
	}

	public int getStart(){
		return start;
	}

	public void setActive(boolean active){
		this.active = active;
	}

	public boolean isActive(){
		return active;
	}

	public void setDescription(String description){
		this.description = description;
	}

	public String getDescription(){
		return description;
	}

	public void setMeetingid(int meetingid){
		this.meetingid = meetingid;
	}

	public int getMeetingid(){
		return meetingid;
	}

	public void setTitle(String title){
		this.title = title;
	}

	public String getTitle(){
		return title;
	}
/*
	public void setUuid(List<String> uuid){
		this.uuid = uuid;
	}

	public List<String> getUuid(){
		return uuid;
	}*/

	public void setUri(String uri){
		this.uri = uri;
	}

	public String getUri(){
		return uri;
	}

	public void setFound(boolean found){
		this.found = found;
	}

	public boolean isFound(){
		return found;
	}

	public void setJsonMemberPublic(boolean jsonMemberPublic){
		this.jsonMemberPublic = jsonMemberPublic;
	}

	public boolean isJsonMemberPublic(){
		return jsonMemberPublic;
	}

	public void setCalendaruri(String calendaruri){
		this.calendaruri = calendaruri;
	}

	public String getCalendaruri(){
		return calendaruri;
	}

	public void setServerUrl(String serverUrl){
		this.serverUrl = serverUrl;
	}

	public String getServerUrl(){
		return serverUrl;
	}

	public void setModpin(String modpin){
		this.modpin = modpin;
	}

	public String getModpin(){
		return modpin;
	}

	public void setEnd(int end){
		this.end = end;
	}

	public int getEnd(){
		return end;
	}

	public void setState(int state){
		this.state = state;
	}

	public int getState(){
		return state;
	}

	public void setVcalendar(String vcalendar){
		this.vcalendar = vcalendar;
	}

	public String getVcalendar(){
		return vcalendar;
	}


	@Override
	public long getId() {
		return 0;
	}

	@Override
	public String getUserId() {
		return null;
	}

	@Override
	public String getUsername() {
		return null;
	}

	@Override
	public String getBaseUrl() {
		return null;
	}

	@Override
	public String getToken() {
		return null;
	}

	@Override
	public String getDisplayName() {
		return null;
	}

	@Override
	public String getPushConfigurationState() {
		return null;
	}

	@Override
	public String getCapabilities() {
		return null;
	}

	@Override
	public String getClientCertificate() {
		return null;
	}

	@Override
	public String getExternalSignalingServer() {
		return null;
	}

	@Override
	public boolean getCurrent() {
		return false;
	}

	@Override
	public boolean getScheduledForDeletion() {
		return false;
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(android.os.Parcel parcel, int i) {

	}
}