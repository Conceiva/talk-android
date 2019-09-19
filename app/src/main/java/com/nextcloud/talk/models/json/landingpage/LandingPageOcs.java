package com.nextcloud.talk.models.json.landingpage;

import com.bluelinelabs.logansquare.annotation.JsonField;
import com.bluelinelabs.logansquare.annotation.JsonObject;

@JsonObject

public class LandingPageOcs {

	@JsonField(name ="data")
	 public LandingResponseData data;

	@JsonField(name ="meta")
	public Meta meta;
}