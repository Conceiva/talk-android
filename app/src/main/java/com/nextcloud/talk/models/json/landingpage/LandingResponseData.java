package com.nextcloud.talk.models.json.landingpage;

import com.bluelinelabs.logansquare.annotation.JsonField;
import com.bluelinelabs.logansquare.annotation.JsonObject;

import org.parceler.Parcel;

import lombok.Data;

@Data
@Parcel
@JsonObject
public class LandingResponseData {

	@JsonField(name ="macosdownload")
	public String macosdownload;

	@JsonField(name ="host_country")
	public  String hostCountry;

	@JsonField(name ="auth")
	public  boolean auth;

	@JsonField(name ="windowsdownload")
	public  String windowsdownload;

	@JsonField(name ="moderator")
	public boolean moderator;

	@JsonField(name ="description")
	public  String description;

	@JsonField(name ="shareid")
	public String shareid;

	@JsonField(name ="title")
	public String title;

	@JsonField(name ="uuid")
	public String uuid;

	@JsonField(name ="found")
	public boolean found;

	@JsonField(name ="protected")
	public boolean jsonMemberProtected;

	@JsonField(name ="usingCookie")
	public boolean usingCookie;

	@JsonField(name ="public_meeting")
	public boolean publicMeeting;

	@JsonField(name ="now")
	public int now;

	/*@JsonField(name ="dialin_nrs")
	 List<DialinNrsItem> dialinNrs;*/

	@JsonField(name ="end")
	public  int end;

	@JsonField(name ="id")
	public String id;

	@JsonField(name ="linuxdownload")
	public  String linuxdownload;

	@JsonField(name ="start")
	public  int start;

	@JsonField(name ="started")
	public  boolean started;

	/*@JsonField(name ="onlyofficeFmts")
	 OnlyofficeFmts onlyofficeFmts;*/

	@JsonField(name ="display_name")
	public  String displayName;


	@JsonField(name ="user_is_host")
	public boolean userIsHost;

	@JsonField(name ="p")
	public String P;

	@JsonField(name ="meeting_session")
	public String meetingSession;


	@JsonField(name ="host_session")
	 public String hostSession;

	@JsonField(name ="allday")
	public boolean allday;

	@JsonField(name ="u")
	public String U;

	@JsonField(name ="ended")
	public  boolean ended;

	@JsonField(name ="scheduled_now")
	public boolean scheduledNow;
}