package com.nextcloud.talk.models.json.landingpage;

import javax.annotation.Generated;
import com.bluelinelabs.logansquare.annotation.JsonField;
import com.bluelinelabs.logansquare.annotation.JsonObject;

import org.parceler.Parcel;

import lombok.Data;

@Generated("com.robohorse.robopojogenerator")
@JsonObject
@Parcel
@Data
public class Meta{

	@JsonField(name ="statuscode")
	 int statuscode;

	@JsonField(name ="message")
	 String message;

	@JsonField(name ="status")
	 String status;
}