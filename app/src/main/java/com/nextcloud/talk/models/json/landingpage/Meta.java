package com.nextcloud.talk.models.json.landingpage;

import javax.annotation.Generated;
import com.bluelinelabs.logansquare.annotation.JsonField;
import com.bluelinelabs.logansquare.annotation.JsonObject;

@Generated("com.robohorse.robopojogenerator")
@JsonObject
public class Meta{

	@JsonField(name ="statuscode")
	 int statuscode;

	@JsonField(name ="message")
	 String message;

	@JsonField(name ="status")
	 String status;
}