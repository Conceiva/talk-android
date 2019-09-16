package com.nextcloud.talk.models.json.landingpage;

import javax.annotation.Generated;
import com.bluelinelabs.logansquare.annotation.JsonField;
import com.bluelinelabs.logansquare.annotation.JsonObject;
import com.nextcloud.talk.models.json.generic.GenericOCS;

@JsonObject
public class LandingPageOcs {

	@JsonField(name ="data")
	 public Data data;

	@JsonField(name ="meta")
	public Meta meta;
}