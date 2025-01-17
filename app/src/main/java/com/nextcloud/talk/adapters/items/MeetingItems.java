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

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.nextcloud.talk.R;
import com.nextcloud.talk.application.NextcloudTalkApplication;
import com.nextcloud.talk.events.MeetingItemClickEvent;
import com.nextcloud.talk.events.MeetingItemJoinMeetingClickEvent;
import com.nextcloud.talk.models.database.UserEntity;
import com.nextcloud.talk.models.json.meetings.MeetingsReponse;
import com.nextcloud.talk.utils.DateUtils;

import net.fortuna.ical4j.data.CalendarBuilder;
import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.Property;
import net.fortuna.ical4j.model.component.VEvent;

import org.greenrobot.eventbus.EventBus;

import java.io.IOException;
import java.io.StringReader;
import java.util.List;
import java.util.regex.Pattern;

import butterknife.BindView;
import butterknife.ButterKnife;
import eu.davidea.flexibleadapter.FlexibleAdapter;
import eu.davidea.flexibleadapter.items.AbstractFlexibleItem;
import eu.davidea.flexibleadapter.items.IFilterable;
import eu.davidea.flexibleadapter.items.IFlexible;
import eu.davidea.flexibleadapter.utils.FlexibleUtils;
import eu.davidea.viewholders.FlexibleViewHolder;

public class MeetingItems extends AbstractFlexibleItem<MeetingItems.ConversationItemViewHolder> implements
        IFilterable<String> {



    private MeetingsReponse meeting;
    private UserEntity userEntity;

    public MeetingItems(MeetingsReponse meeting, UserEntity userEntity) {
        this.meeting = meeting;
        this.userEntity = userEntity;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof MeetingItems) {
            MeetingItems inItem = (MeetingItems) o;
            return meeting.equals(inItem.getModel());
        }
        return false;
    }

    public MeetingsReponse getModel() {
        return meeting;
    }

    @Override
    public int hashCode() {
        return meeting.hashCode();
    }

    @Override
    public int getLayoutRes() {
        return R.layout.rv_item_meeting;
    }

    @Override
    public ConversationItemViewHolder createViewHolder(View view, FlexibleAdapter<IFlexible> adapter) {
        return new ConversationItemViewHolder(view, adapter);
    }

    @Override
    public void bindViewHolder(FlexibleAdapter<IFlexible> adapter, ConversationItemViewHolder holder, int position, List<Object> payloads) {
        Context context = NextcloudTalkApplication.Companion.getSharedApplication().getApplicationContext();


        if (adapter.hasFilter()) {
            FlexibleUtils.highlightText(holder.meetingTitle, meeting.getTitle(),
                    String.valueOf(adapter.getFilter(String.class)), NextcloudTalkApplication.Companion.getSharedApplication()
                            .getResources().getColor(R.color.colorPrimary));
        } else {
            holder.meetingTitle.setText(meeting.getTitle());
        }


        if (meeting.isOwner()) {
            holder.meetingHostLinearLayout.setVisibility(View.VISIBLE);
            holder.meetingHostTextView.setText("Host");
        } else {
            holder.meetingHostLinearLayout.setVisibility(View.GONE);
        }

        holder.meetingDateTextView.setText(DateUtils.INSTANCE.getDateTimeStringFromTimestamp(meeting.getStart(), "dd MMMM yyyy", ""));
        holder.meetingTimeTextView.setText(DateUtils.INSTANCE.getDateTimeStringFromTimestamp(meeting.getStart(), "HH:mm", ""));

        if (meeting.isJsonMemberPublic()) {
            holder.meetingType.setText(context.getString(R.string.str_public));
        } else {
            holder.meetingType.setText(context.getString(R.string.str_private));
        }

        if (meeting.getDescription().isEmpty()) {
            holder.meetingDescription.setVisibility(View.GONE);
        } else {
            holder.meetingDescription.setVisibility(View.VISIBLE);
            holder.meetingDescription.setText(meeting.getDescription());

        }
        holder.meetingFrequencyTextView.setVisibility(View.GONE);

        holder.meetingID.setText(context.getResources().getString(R.string.str_meeting_id) + " " + meeting.getMeetingid());


        StringReader sin = new StringReader(meeting.getVcalendar());

        CalendarBuilder builder = new CalendarBuilder();
        Calendar cal = null;
        String meetingURL = "";
        Property propertyMeetingURL = null;
        try {
            cal = builder.build(sin);
            VEvent component = (VEvent) cal.getComponents().getComponent("VEVENT");
            propertyMeetingURL = component.getProperties().getProperty("URL");
            Property rrule = component.getProperties().getProperty("RRULE");
            if (rrule != null) {
                String rule = rrule.getValue().toString();
                String startingOn = " Starting on " + DateUtils.INSTANCE.getDateTimeStringFromTimestamp(meeting.getStart(), "dd MMMM yyyy HH:mm", meeting.getTimezone());
                if (getTextForFrequency(rule).equalsIgnoreCase("")) {
                    holder.meetingFrequencyTextView.setVisibility(View.GONE);
                } else {
                    holder.meetingFrequencyTextView.setVisibility(View.VISIBLE);
                    holder.meetingFrequencyTextView.setText(getTextForFrequency(rule) + " " + startingOn);
                }
            } else {
                holder.meetingFrequencyTextView.setVisibility(View.GONE);
            }
            if (propertyMeetingURL != null)
                meetingURL = propertyMeetingURL.getValue();
            //FREQ=YEARLY;BYMONTH=4;BYDAY=1SU
        } catch (IOException e) {
            if (propertyMeetingURL != null)
                meetingURL = propertyMeetingURL.getValue();
            Log.d("calendar", "io exception" + e.getLocalizedMessage());
        } catch (Exception e) {
            if (propertyMeetingURL != null)
                meetingURL = propertyMeetingURL.getValue();
            Log.d("calendar", "parser exception" + e.getLocalizedMessage());
        }

        holder.viewDetailsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                EventBus.getDefault().post(new MeetingItemClickEvent(meeting));
            }
        });
        holder.joinMeetingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                EventBus.getDefault().post(new MeetingItemJoinMeetingClickEvent(meeting));
            }
        });

        if (meeting.isJsonMemberPublic()) {
            meetingURL = meeting.getServerUrl() + "jc/" + meeting.getMeetingid();
            holder.copyRelativeLayout.setVisibility(View.VISIBLE);
            holder.copyTextView.setText(meetingURL);
        } else {
            holder.copyRelativeLayout.setVisibility(View.GONE);
        }
        if(meeting.isActive())
        {
            holder.activeLinearLayout.setVisibility(View.VISIBLE);
        }
        else {
            holder.activeLinearLayout.setVisibility(View.GONE);
        }

        holder.copyRelativeLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("Meeting Link", holder.copyTextView.getText().toString());
                clipboard.setPrimaryClip(clip);
                Toast.makeText(context, context.getResources().getString(R.string.url_copied), Toast.LENGTH_LONG).show();
            }
        });


    }

    private String getTextForFrequency(String rule) {
        String[] splitted = rule.split(";");
        String frequency = "", interval = "";

        String finalText = "";
        for (int i = 0; i < splitted.length; i++) {
            if (splitted[i].contains("FREQ")) {
                frequency = (splitted[i].split("="))[1];
            }

            if (splitted[i].contains("INTERVAL")) {
                interval = ((splitted[i].split("="))[1]);
            }
        }

        if (interval.equalsIgnoreCase("0"))
            return "";

        if (frequency.equalsIgnoreCase("DAILY")) {
            if (interval.equalsIgnoreCase("1") || interval.equalsIgnoreCase("")) {
                finalText = "Every Day";
            } else {
                finalText = "Every " + interval + " day";
            }
        } else if (frequency.equalsIgnoreCase("WEEKLY")) {
            if (interval.equalsIgnoreCase("1") || interval.equalsIgnoreCase("")) {
                finalText = "Every Week";
            } else {
                finalText = "Every " + interval + " Weeks";
            }
        } else if (frequency.equalsIgnoreCase("MONTHLY")) {
            if (interval.equalsIgnoreCase("1") || interval.equalsIgnoreCase("")) {
                finalText = "Every Month";
            } else {
                finalText = "Every " + interval + " Months";
            }
        } else if (frequency.equalsIgnoreCase("YEARLY")) {
            if (interval.equalsIgnoreCase("1") || interval.equalsIgnoreCase("")) {
                finalText = "Every Year";
            } else {
                finalText = "Every " + interval + " Years";
            }
        }
        return finalText;

    }

    @Override
    public boolean filter(String constraint) {
        return meeting.getTitle() != null &&
                Pattern.compile(constraint, Pattern.CASE_INSENSITIVE | Pattern.LITERAL).matcher(meeting.getTitle()).find();
    }

    static class ConversationItemViewHolder extends FlexibleViewHolder {

        @BindView(R.id.meetingTitle)
        TextView meetingTitle;
        @BindView(R.id.meetingType)
        TextView meetingType;
        @BindView(R.id.meetingID)
        TextView meetingID;
        @BindView(R.id.meetingTitleRelativeLayout)
        RelativeLayout meetingTitleRelativeLayout;
        @BindView(R.id.passwordProtectedRoomImageView)
        ImageView passwordProtectedRoomImageView;
        @BindView(R.id.meetingDateTextView)
        TextView meetingDateTextView;
        @BindView(R.id.meetingDateLinearLayout)
        LinearLayout meetingDateLinearLayout;
        @BindView(R.id.meetingTimeImageView)
        ImageView meetingTimeImageView;
        @BindView(R.id.meetingTimeTextView)
        TextView meetingTimeTextView;
        @BindView(R.id.meetingTimeLinearLayout)
        LinearLayout meetingTimeLinearLayout;
        @BindView(R.id.meetingHostImageView)
        ImageView meetingHostImageView;
        @BindView(R.id.meetingHostTextView)
        TextView meetingHostTextView;
        @BindView(R.id.meetingHostLinearLayout)
        LinearLayout meetingHostLinearLayout;
        @BindView(R.id.meetingFrequencyTextView)
        TextView meetingFrequencyTextView;
        @BindView(R.id.meetingDescription)
        TextView meetingDescription;
        @BindView(R.id.viewDetailsButton)
        Button viewDetailsButton;
        @BindView(R.id.joinMeetingButton)
        Button joinMeetingButton;
        @BindView(R.id.copyImageView)
        ImageView copyImageView;
        @BindView(R.id.copyTextView)
        TextView copyTextView;
        @BindView(R.id.copyFileImageView)
        ImageView copyFileImageView;
        @BindView(R.id.copyRelativeLayout)
        RelativeLayout copyRelativeLayout;
        @BindView(R.id.activeLinearLayout)
        LinearLayout activeLinearLayout;
        ConversationItemViewHolder(View view, FlexibleAdapter adapter) {
            super(view, adapter);
            ButterKnife.bind(this, view);
        }
    }
}
