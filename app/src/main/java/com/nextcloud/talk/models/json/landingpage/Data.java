package com.nextcloud.talk.models.json.landingpage;

import com.bluelinelabs.logansquare.annotation.JsonField;
import com.bluelinelabs.logansquare.annotation.JsonObject;

@JsonObject
public class Data{

	@JsonField(name ="macosdownload")
	 String macosdownload;

	@JsonField(name ="host_country")
	 String hostCountry;

	@JsonField(name ="auth")
	 boolean auth;

	@JsonField(name ="windowsdownload")
	 String windowsdownload;

	@JsonField(name ="moderator")
	 boolean moderator;

	@JsonField(name ="description")
	 String description;

	@JsonField(name ="shareid")
	 String shareid;

	@JsonField(name ="title")
	 String title;

	@JsonField(name ="uuid")
	 String uuid;

	@JsonField(name ="found")
	 boolean found;

	@JsonField(name ="protected")
	 boolean jsonMemberProtected;

	@JsonField(name ="usingCookie")
	 boolean usingCookie;

	@JsonField(name ="public_meeting")
	 boolean publicMeeting;

	@JsonField(name ="now")
	 int now;

	/*@JsonField(name ="dialin_nrs")
	 List<DialinNrsItem> dialinNrs;*/

	@JsonField(name ="end")
	 int end;

	@JsonField(name ="id")
	 String id;

	@JsonField(name ="linuxdownload")
	 String linuxdownload;

	@JsonField(name ="start")
	 int start;

	@JsonField(name ="started")
	 boolean started;

	/*@JsonField(name ="onlyofficeFmts")
	 OnlyofficeFmts onlyofficeFmts;*/

	@JsonField(name ="display_name")
	 String displayName;


	@JsonField(name ="user_is_host")
	 boolean userIsHost;

	@JsonField(name ="p")
	 Object P;

	@JsonField(name ="meeting_session")
	public String meetingSession;

	@JsonField(name ="host_session")
	 String hostSession;

	@JsonField(name ="allday")
	 boolean allday;

	@JsonField(name ="u")
	 String U;

	@JsonField(name ="ended")
	 boolean ended;

	@JsonField(name ="scheduled_now")
	 boolean scheduledNow;
}