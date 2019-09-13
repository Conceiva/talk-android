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

package com.nextcloud.talk.adapters.items;

import android.content.res.Resources;
import android.graphics.drawable.ColorDrawable;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.facebook.drawee.view.SimpleDraweeView;
import com.nextcloud.talk.R;
import com.nextcloud.talk.application.NextcloudTalkApplication;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import eu.davidea.flexibleadapter.FlexibleAdapter;
import eu.davidea.flexibleadapter.items.AbstractFlexibleItem;
import eu.davidea.flexibleadapter.items.IFlexible;
import eu.davidea.viewholders.FlexibleViewHolder;

public class MetingAttendeeItem extends AbstractFlexibleItem<MetingAttendeeItem.NotificationSoundItemViewHolder> {

    private String attendeeName;
    private String attendeeInfo;

    public MetingAttendeeItem(String attendeeName, String attendeeInfo) {
        this.attendeeName = attendeeName;
        this.attendeeInfo = attendeeInfo;
    }

    public String getAttendeeName() {
        return attendeeName;
    }

    public String getAttendeeInfo() {
        return attendeeInfo;
    }

    @Override
    public boolean equals(Object o) {
        return false;
    }

    @Override
    public int getLayoutRes() {
        return R.layout.rv_item_attendee;
    }

    @Override
    public NotificationSoundItemViewHolder createViewHolder(View view, FlexibleAdapter<IFlexible> adapter) {
        return new NotificationSoundItemViewHolder(view, adapter);
    }

    @Override
    public void bindViewHolder(FlexibleAdapter<IFlexible> adapter, NotificationSoundItemViewHolder holder, int position, List<Object> payloads) {
        holder.attandee_name.setText(attendeeName+" "+attendeeInfo);


        Resources resources = NextcloudTalkApplication.Companion.getSharedApplication().getResources();

    }

    static class NotificationSoundItemViewHolder extends FlexibleViewHolder {
        @BindView(R.id.attandee_name)
        public TextView attandee_name;

        @BindView(R.id.img_cancel)
        ImageButton img_cancel;

        /**
         * Default constructor.
         */
        NotificationSoundItemViewHolder(View view, FlexibleAdapter adapter) {
            super(view, adapter);
            ButterKnife.bind(this, view);
        }
    }


}
