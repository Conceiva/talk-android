package com.nextcloud.talk.models.json.landingpage;

import javax.annotation.Generated;
import com.bluelinelabs.logansquare.annotation.JsonField;
import com.bluelinelabs.logansquare.annotation.JsonObject;
import com.nextcloud.talk.models.json.generic.GenericOCS;

@JsonObject
public class LandingPageResponse extends GenericOCS {

	@JsonField(name ="ocs")
	public  LandingPageOcs landingPageOcs;
}