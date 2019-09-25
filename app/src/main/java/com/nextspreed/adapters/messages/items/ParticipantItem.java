/*
 * Nextcloud Talk application
 *
 * @author Mario Danic
 * Copyright (C) 2017 Mario Danic (mario@lovelyhq.com)
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

package com.nextspreed.adapters.messages.items;

import android.content.res.Resources;
import android.graphics.drawable.BitmapDrawable;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import androidx.emoji.widget.EmojiTextView;

import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.drawee.interfaces.DraweeController;
import com.facebook.drawee.view.SimpleDraweeView;
import com.nextcloud.talk.R;
import com.nextcloud.talk.adapters.items.GenericTextHeaderItem;
import com.nextcloud.talk.application.NextcloudTalkApplication;

import com.nextcloud.talk.models.database.UserEntity;
import com.nextcloud.talk.models.json.converters.EnumParticipantTypeConverter;
import com.nextcloud.talk.models.json.participants.Participant;
import com.nextcloud.talk.utils.ApiUtils;
import com.nextcloud.talk.utils.DisplayUtils;

import java.util.List;
import java.util.regex.Pattern;

import javax.annotation.Nullable;

import butterknife.BindView;
import butterknife.ButterKnife;
import eu.davidea.flexibleadapter.FlexibleAdapter;
import eu.davidea.flexibleadapter.items.AbstractFlexibleItem;
import eu.davidea.flexibleadapter.items.IFilterable;
import eu.davidea.flexibleadapter.items.ISectionable;
import eu.davidea.flexibleadapter.utils.FlexibleUtils;
import eu.davidea.viewholders.FlexibleViewHolder;

public class ParticipantItem extends AbstractFlexibleItem<ParticipantItem.UserItemViewHolder> implements
        ISectionable<ParticipantItem.UserItemViewHolder, GenericTextHeaderItem>, IFilterable<String> {

    private Participant participant;
    private UserEntity userEntity;
    private GenericTextHeaderItem header;

    public ParticipantItem(Participant participant, UserEntity userEntity, GenericTextHeaderItem genericTextHeaderItem) {
        this.participant = participant;
        this.userEntity = userEntity;
        this.header = genericTextHeaderItem;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof ParticipantItem) {
            ParticipantItem inItem = (ParticipantItem) o;
            return participant.userId.equals(inItem.getModel().userId);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return participant.hashCode();
    }

    /**
     * @return the model object
     */

    public Participant getModel() {
        return participant;
    }

    public UserEntity getEntity() {
        return userEntity;
    }


    @Override
    public int getLayoutRes() {

        return R.layout.rv_spreed_item_conversation_info_participant;
    }

    @Override
    public UserItemViewHolder createViewHolder(View view, FlexibleAdapter adapter) {
        return new UserItemViewHolder(view, adapter);
    }

    @Override
    public void bindViewHolder(FlexibleAdapter adapter, UserItemViewHolder holder, int position, List payloads) {

        holder.simpleDraweeView.setController(null);

        if (participant.displayName != null && !TextUtils.isEmpty(participant.displayName)) {
            holder.participantParentLayout.setVisibility(View.VISIBLE);
            if (holder.checkedImageView != null) {
                if (participant.selected) {
                    holder.checkedImageView.setVisibility(View.VISIBLE);
                } else {
                    holder.checkedImageView.setVisibility(View.GONE);
                }
            }

            if (adapter.hasFilter()) {
                FlexibleUtils.highlightText(holder.contactDisplayName, participant.displayName,
                        String.valueOf(adapter.getFilter(String.class)), NextcloudTalkApplication.Companion.getSharedApplication()
                                .getResources().getColor(R.color.colorPrimary));
            }

            holder.contactDisplayName.setText(participant.displayName);

            if (TextUtils.isEmpty(participant.displayName) &&
                    (participant.type.equals(Participant.ParticipantType.GUEST) || participant.type.equals(Participant.ParticipantType.USER_FOLLOWING_LINK))) {
                holder.contactDisplayName.setText(NextcloudTalkApplication.Companion.getSharedApplication().getString(R.string.nc_guest));
            }

            if (TextUtils.isEmpty(participant.source) || participant.source.equals("users")) {

                if (Participant.ParticipantType.USER_FOLLOWING_LINK.equals(participant.type)) {
                    String displayName = NextcloudTalkApplication.Companion.getSharedApplication()
                            .getResources().getString(R.string.nc_guest);

                    if (!TextUtils.isEmpty(participant.displayName)) {
                        displayName = participant.displayName;
                    }

                    DraweeController draweeController = Fresco.newDraweeControllerBuilder()
                            .setOldController(holder.simpleDraweeView.getController())
                            .setAutoPlayAnimations(true)
                            .setImageRequest(DisplayUtils.getImageRequestForUrl(ApiUtils.getUrlForAvatarWithNameForGuests(userEntity.getBaseUrl(),
                                    displayName, R.dimen.avatar_size), null))
                            .build();
                    holder.simpleDraweeView.setController(draweeController);

                } else {

                    DraweeController draweeController = Fresco.newDraweeControllerBuilder()
                            .setOldController(holder.simpleDraweeView.getController())
                            .setAutoPlayAnimations(true)
                            .setImageRequest(DisplayUtils.getImageRequestForUrl(ApiUtils.getUrlForAvatarWithName(userEntity.getBaseUrl(),
                                    participant.userId, R.dimen.avatar_size), null))
                            .build();
                    holder.simpleDraweeView.setController(draweeController);

                }
            } else if ("groups".equals(participant.source)) {
                holder.simpleDraweeView.getHierarchy().setImage(new BitmapDrawable(DisplayUtils.getRoundedBitmapFromVectorDrawableResource(NextcloudTalkApplication.Companion.getSharedApplication().getResources(), R.drawable.ic_people_group_white_24px)), 100, true);
            }

            if (!isEnabled()) {
                holder.itemView.setAlpha(0.38f);
            } else {
                holder.itemView.setAlpha(1.0f);
            }

            Resources resources = NextcloudTalkApplication.Companion.getSharedApplication().getResources();

            if (header == null && ((Boolean)participant.inCall))
            {
                holder.buttonRelativeLayout.setVisibility(View.VISIBLE);
                Participant.AudioFlags audioFlags = participant.audioStatus;
                Participant.VideoFlags videoFlag = participant.videoStatus;

                switch (audioFlags) {
                    case DISABLED:
                        holder.voiceOrSimpleCallImageView.setImageResource(R.drawable.ic_mic_off_white_24px);
                        break;
                    case ENABLED:
                        holder.voiceOrSimpleCallImageView.setImageResource(R.drawable.ic_mic_white_24px);
                        break;
                    case SPEAKING:
                        holder.voiceOrSimpleCallImageView.setImageResource(R.drawable.ic_mic_talking);
                        break;
                    default:
                        holder.voiceOrSimpleCallImageView.setImageResource(R.drawable.ic_mic_grey_600_24dp);
                        break;
                }

                switch (videoFlag) {
                    case DISABLED:
                        holder.videoCallImageView.setImageResource(R.drawable.ic_videocam_off_white_24px);

                        break;
                    case ENABLED:

                        holder.videoCallImageView.setImageResource(R.drawable.ic_videocam_white_24px);

                        break;
                    case SPEAKING:
                        holder.videoCallImageView.setImageResource(R.drawable.ic_videocam_video);
                        break;
                    default:
                        holder.videoCallImageView.setImageResource(R.drawable.ic_videocam_grey_600_24dp);
                        break;
                }

                if (holder.contactMentionId != null) {
                    String userType = "";

                    switch (new EnumParticipantTypeConverter().convertToInt(participant.getType())) {
                        case 1:
                            //userType = NextcloudTalkApplication.Companion.getSharedApplication().getString(R.string.nc_owner);
                            //break;
                        case 2:
                            userType = NextcloudTalkApplication.Companion.getSharedApplication().getString(R.string.nc_moderator);
                            break;
                        case 3:
                            userType = NextcloudTalkApplication.Companion.getSharedApplication().getString(R.string.nc_user);
                            break;
                        case 4:
                            userType = NextcloudTalkApplication.Companion.getSharedApplication().getString(R.string.nc_guest);
                            break;
                        case 5:
                            userType = NextcloudTalkApplication.Companion.getSharedApplication().getString(R.string.nc_following_link);
                            break;
                        default:
                            break;
                    }

                    if (!holder.contactMentionId.getText().equals(userType)) {
                        holder.contactMentionId.setText(userType);
                        holder.contactMentionId.setTextColor(NextcloudTalkApplication.Companion.getSharedApplication().getResources().getColor(R.color.colorPrimary));
                    }
                }
            }
            else {
                holder.buttonRelativeLayout.setVisibility(View.GONE);
            }
        } else {
            holder.participantParentLayout.setVisibility(View.GONE);
        }
    }

    @Override
    public boolean filter(String constraint) {
        return participant.displayName != null &&
                (Pattern.compile(constraint, Pattern.CASE_INSENSITIVE | Pattern.LITERAL).matcher(participant.displayName.trim()).find() ||
                        Pattern.compile(constraint, Pattern.CASE_INSENSITIVE | Pattern.LITERAL).matcher(participant.userId.trim()).find());
    }

    @Override
    public GenericTextHeaderItem getHeader() {
        return header;
    }

    @Override
    public void setHeader(GenericTextHeaderItem header) {
        this.header = header;
    }


    static class UserItemViewHolder extends FlexibleViewHolder {

        @BindView(R.id.name_text)
        public EmojiTextView contactDisplayName;
        @BindView(R.id.buttonRelativeLayout)
        public RelativeLayout buttonRelativeLayout;

        @BindView(R.id.participanrParentLayout)
        public RelativeLayout participantParentLayout;
        @BindView(R.id.simple_drawee_view)
        public SimpleDraweeView simpleDraweeView;
        @Nullable
        @BindView(R.id.secondary_text)
        public EmojiTextView contactMentionId;
        @Nullable
        @BindView(R.id.voiceOrSimpleCallImageView)
        ImageView voiceOrSimpleCallImageView;
        @Nullable
        @BindView(R.id.videoCallImageView)
        ImageView videoCallImageView;
        @Nullable
        @BindView(R.id.checkedImageView)
        ImageView checkedImageView;

        /**
         * Default constructor.
         */
        UserItemViewHolder(View view, FlexibleAdapter adapter) {
            super(view, adapter);
            ButterKnife.bind(this, view);
        }
    }


}
