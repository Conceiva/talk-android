/*
 * Nextcloud Talk application
 *
 * @author Mario Danic
 * Copyright (C) 2017-2019 Mario Danic <mario@lovelyhq.com>
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

package com.nextcloud.talk.controllers;


import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.emoji.widget.EmojiEditText;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bluelinelabs.conductor.RouterTransaction;
import com.bluelinelabs.conductor.changehandler.HorizontalChangeHandler;
import com.bluelinelabs.conductor.changehandler.VerticalChangeHandler;
import com.bluelinelabs.logansquare.LoganSquare;
import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.drawee.interfaces.DraweeController;
import com.facebook.drawee.view.SimpleDraweeView;
import com.nextcloud.talk.R;
import com.nextcloud.talk.activities.MagicCallActivity;


import com.nextcloud.talk.api.NcApi;
import com.nextcloud.talk.application.NextcloudTalkApplication;
import com.nextcloud.talk.callbacks.MentionAutocompleteCallback;
import com.nextcloud.talk.components.filebrowser.controllers.BrowserController;
import com.nextcloud.talk.controllers.base.BaseController;
import com.nextcloud.talk.events.UserMentionClickEvent;
import com.nextcloud.talk.events.WebSocketCommunicationEvent;
import com.nextcloud.talk.events.WebSocketMessageEvent;
import com.nextcloud.talk.models.ExternalSignalingServer;
import com.nextcloud.talk.models.RetrofitBucket;
import com.nextcloud.talk.models.database.UserEntity;
import com.nextcloud.talk.models.json.call.Call;
import com.nextcloud.talk.models.json.call.CallOverall;
import com.nextcloud.talk.models.json.capabilities.CapabilitiesOverall;
import com.nextcloud.talk.models.json.chat.ChatMessage;
import com.nextcloud.talk.models.json.chat.ChatOverall;
import com.nextcloud.talk.models.json.conversations.Conversation;
import com.nextcloud.talk.models.json.conversations.RoomOverall;
import com.nextcloud.talk.models.json.conversations.RoomsOverall;
import com.nextcloud.talk.models.json.generic.GenericMeta;
import com.nextcloud.talk.models.json.generic.GenericOverall;
import com.nextcloud.talk.models.json.landingpage.LandingResponseData;
import com.nextcloud.talk.models.json.mention.Mention;
import com.nextcloud.talk.models.json.participants.Participant;
import com.nextcloud.talk.models.json.signaling.settings.IceServer;
import com.nextcloud.talk.models.json.signaling.settings.SignalingSettingsOverall;
import com.nextcloud.talk.presenters.MentionAutocompletePresenter;
import com.nextcloud.talk.utils.ApiUtils;
import com.nextcloud.talk.utils.ConductorRemapping;
import com.nextcloud.talk.utils.CustomProgressDialog;
import com.nextcloud.talk.utils.DateUtils;
import com.nextcloud.talk.utils.DialogUtils;
import com.nextcloud.talk.utils.DisplayUtils;
import com.nextcloud.talk.utils.KeyboardUtils;
import com.nextcloud.talk.utils.MagicCharPolicy;
import com.nextcloud.talk.utils.PreferenceHelper;
import com.nextcloud.talk.utils.bundle.BundleKeys;
import com.nextcloud.talk.utils.database.user.UserUtils;
import com.nextcloud.talk.utils.preferences.AppPreferences;
import com.nextcloud.talk.utils.singletons.ApplicationWideCurrentRoomHolder;
import com.nextcloud.talk.utils.text.Spans;
import com.nextcloud.talk.webrtc.MagicWebSocketInstance;
import com.nextcloud.talk.webrtc.WebSocketConnectionHelper;
import com.nextspreed.adapters.messages.MagicIncomingTextMessageViewHolder;
import com.nextspreed.adapters.messages.MagicOutcomingTextMessageViewHolder;
import com.nextspreed.adapters.messages.MagicPreviewMessageViewHolder;
import com.nextspreed.adapters.messages.MagicSystemMessageViewHolder;
import com.nextspreed.controllers.ConversationParticipantsController;
import com.otaliastudios.autocomplete.Autocomplete;
import com.otaliastudios.autocomplete.AutocompleteCallback;
import com.otaliastudios.autocomplete.AutocompletePresenter;
import com.stfalcon.chatkit.commons.ImageLoader;
import com.stfalcon.chatkit.commons.models.IMessage;
import com.stfalcon.chatkit.messages.MessageHolders;
import com.stfalcon.chatkit.messages.MessageInput;
import com.stfalcon.chatkit.messages.MessagesList;
import com.stfalcon.chatkit.messages.MessagesListAdapter;
import com.stfalcon.chatkit.utils.DateFormatter;
import com.vanniktech.emoji.EmojiImageView;
import com.vanniktech.emoji.EmojiPopup;
import com.vanniktech.emoji.emoji.Emoji;
import com.vanniktech.emoji.listeners.OnEmojiClickListener;
import com.vanniktech.emoji.listeners.OnEmojiPopupDismissListener;
import com.vanniktech.emoji.listeners.OnEmojiPopupShownListener;
import com.webianks.library.PopupBubble;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.parceler.Parcels;
import org.webrtc.PeerConnection;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import autodagger.AutoInjector;
import butterknife.BindView;
import butterknife.OnClick;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import retrofit2.HttpException;
import retrofit2.Response;

@AutoInjector(NextcloudTalkApplication.class)
public class ChatControllerSpreed extends BaseController implements MessagesListAdapter.OnLoadMoreListener,
        MessagesListAdapter.Formatter<Date>, MessagesListAdapter.OnMessageLongClickListener, MessageHolders.ContentChecker {
    private static final String TAG = "ChatController";
    private static final byte CONTENT_TYPE_SYSTEM_MESSAGE = 1;
    @Inject
    NcApi ncApi;
    @Inject
    UserUtils userUtils;
    @Inject
    AppPreferences appPreferences;
    @Inject
    Context context;
    @Inject
    EventBus eventBus;
    @BindView(R.id.messagesListView)
    MessagesList messagesListView;
    @BindView(R.id.messageInputView)
    MessageInput messageInputView;
    @BindView(R.id.messageInput)
    EmojiEditText messageInput;
    @BindView(R.id.popupBubbleView)
    PopupBubble popupBubble;
    @BindView(R.id.progressBar)
    ProgressBar loadingProgressBar;
    @BindView(R.id.smileyButton)
    ImageButton smileyButton;
    @BindView(R.id.lobby_view_spreed)
    LinearLayout lobbyView;

    @BindView(R.id.messageTextView)
    TextView conversationLobbyText;

    @BindView(R.id.nameTextView)
    TextView nameTextView;

    @BindView(R.id.meetingIdValueTextView)
    TextView meetingIdValueTextView;

    @BindView(R.id.meetingTitleValueTextView)
    TextView meetingTitleValueTextView;

    @BindView(R.id.meetingDetailsValueTextView)
    TextView meetingDetailsValueTextView;

    @BindView(R.id.meetingStartValueTextView)
    TextView meetingStartValueTextView;

    @BindView(R.id.meetingEndValueTextView)
    TextView meetingEndValueTextView;

    @BindView(R.id.meetingTimeZoneValueTextView)
    TextView meetingTimeZoneValueTextView;

    @BindView(R.id.emptyLayout)
    RelativeLayout emptyLayout;

    private List<Disposable> disposableList = new ArrayList<>();
    private String conversationName;
    private String roomToken;
    private UserEntity conversationUser;
    private String roomPassword;
    private String credentials;
    private Conversation currentConversation;
    private Call currentCall;
    private boolean inChat = false;
    private boolean historyRead = false;
    private int globalLastKnownFutureMessageId = -1;
    private int globalLastKnownPastMessageId = -1;
    private MessagesListAdapter<ChatMessage> adapter;
    private Autocomplete mentionAutocomplete;
    private LinearLayoutManager layoutManager;
    private boolean lookingIntoFuture = false;
    private int newMessagesCount = 0;
    private Boolean startCallFromNotification = null;
    private String roomId;
    private boolean voiceOnly;
    private boolean isFirstMessagesProcessing = true;
    private boolean isLeavingForConversation;
    private boolean isLinkPreviewAllowed;
    private boolean wasDetached;
    private EmojiPopup emojiPopup;

    private CharSequence myFirstMessage;
    private boolean checkingLobbyStatus;

    private MenuItem conversationInfoMenuItem;
    private MenuItem conversationVoiceCallMenuItem;
    private MenuItem conversationVideoMenuItem;

    private MagicWebSocketInstance magicWebSocketInstance;

    private Handler lobbyTimerHandler;
    LandingResponseData landingPageResponse;

    //Call Parameters
    private boolean hasExternalSignalingServer;
    private ExternalSignalingServer externalSignalingServer;
    private List<PeerConnection.IceServer> iceServers;
    private boolean isMultiSession = false;
    private boolean needsPing = true;
    private MagicWebSocketInstance webSocketClient;
    private WebSocketConnectionHelper webSocketConnectionHelper;
    private String callSession;
    List<ChatMessage> chatMessageList;
    boolean isMessageAddedFromSocket = false;

    public ChatControllerSpreed(Bundle args) {
        super(args);
        setHasOptionsMenu(true);
        NextcloudTalkApplication.Companion.getSharedApplication().getComponentApplication().inject(this);

        this.conversationUser = args.getParcelable(BundleKeys.INSTANCE.getKEY_USER_ENTITY());
        this.roomId = args.getString(BundleKeys.INSTANCE.getKEY_ROOM_ID(), "");
        this.roomToken = args.getString(BundleKeys.INSTANCE.getKEY_ROOM_TOKEN(), "");

        if (args.containsKey(BundleKeys.INSTANCE.getKEY_ACTIVE_CONVERSATION())) {
            this.currentConversation = Parcels.unwrap(args.getParcelable(BundleKeys.INSTANCE.getKEY_ACTIVE_CONVERSATION()));
            if (currentConversation != null) {
                conversationName = currentConversation.displayName;
            }
        }

        if (args.containsKey(BundleKeys.INSTANCE.getKEY_LANDING_PAGE_RESPONSE())) {
            this.landingPageResponse = Parcels.unwrap(args.getParcelable(BundleKeys.INSTANCE.getKEY_LANDING_PAGE_RESPONSE()));
            if (landingPageResponse != null) {
            }
        }

        this.roomPassword = args.getString(BundleKeys.INSTANCE.getKEY_CONVERSATION_PASSWORD(), "");

       /* if (conversationUser.getUserId().equals("?")) {
            credentials = null;
        } else {*/
        credentials = ApiUtils.getCredentials(conversationUser.getUsername(), conversationUser.getToken());
//        }

        if (args.containsKey(BundleKeys.INSTANCE.getKEY_FROM_NOTIFICATION_START_CALL())) {
            this.startCallFromNotification = args.getBoolean(BundleKeys.INSTANCE.getKEY_FROM_NOTIFICATION_START_CALL());
        }

        this.voiceOnly = args.getBoolean(BundleKeys.INSTANCE.getKEY_CALL_VOICE_ONLY(), false);
    }

    private void getRoomInfo() {
        boolean shouldRepeat = conversationUser.hasSpreedFeatureCapability("webinary-lobby");
        if (shouldRepeat) {
            checkingLobbyStatus = true;
        }

        ncApi.getRoom(credentials, ApiUtils.getRoom(conversationUser.getBaseUrl(), roomToken))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<RoomOverall>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        disposableList.add(d);
                    }

                    @Override
                    public void onNext(RoomOverall roomOverall) {
                        Conversation oldConversation = null;
                        checkingLobbyStatus = true;
                        if (currentConversation != null) {
                            oldConversation = currentConversation;
                        }

                        currentConversation = roomOverall.getOcs().getData();
                        //MAulik
//                        loadAvatarForStatusBar();

                        conversationName = currentConversation.getDisplayName();
                        setTitle();
                        setupMentionAutocomplete();

                        checkReadOnlyState();
                        checkLobbyState(false);
                        setupWebsocket();
                      /*  if (oldConversation == null || oldConversation.getRoomId() == null) {
                            joinRoomWithPassword();
                        }*/
//                        if (oldConversation == null || oldConversation.getRoomId() == null) {
                        joinRoomWithPassword();
//                        }

                    }

                    @Override
                    public void onError(Throwable e) {
                        Toast.makeText(context, context.getResources().getString(R.string.err_room_info), Toast.LENGTH_LONG).show();
                    }

                    @Override
                    public void onComplete() {
                        checkingLobbyStatus = true;
                        if (shouldRepeat) {
                            if (lobbyTimerHandler == null) {
                                lobbyTimerHandler = new Handler();
                            }

//                            lobbyTimerHandler.postDelayed(() -> getRoomInfo(), 5000);
                        }
                    }
                });
    }

    private void handleFromNotification() {
        ncApi.getRooms(credentials, ApiUtils.getUrlForGetRooms(conversationUser.getBaseUrl()))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<RoomsOverall>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        disposableList.add(d);
                    }

                    @Override
                    public void onNext(RoomsOverall roomsOverall) {
                        for (Conversation conversation : roomsOverall.getOcs().getData()) {
                            if (roomId.equals(conversation.getRoomId())) {
                                roomToken = conversation.getToken();
                                currentConversation = conversation;
                                checkLobbyState(false);
                                checkReadOnlyState();
                                conversationName = conversation.getDisplayName();
                                setTitle();
                                break;
                            }
                        }

                        if (!TextUtils.isEmpty(roomToken)) {
                            setupMentionAutocomplete();
                            joinRoomWithPassword();
                        }
                    }

                    @Override
                    public void onError(Throwable e) {

                    }

                    @Override
                    public void onComplete() {

                    }
                });
    }

    @Override
    protected View inflateView(@NonNull LayoutInflater inflater, @NonNull ViewGroup container) {
        return inflater.inflate(R.layout.controller_chat_spreed, container, false);
    }

    @Override
    protected void onViewBound(@NonNull View view) {
        super.onViewBound(view);
        isMessageAddedFromSocket = false;
        getActionBar().show();
        boolean adapterWasNull = false;

        if (adapter == null) {

            loadingProgressBar.setVisibility(View.VISIBLE);

            adapterWasNull = true;

            MessageHolders messageHolders = new MessageHolders();
            messageHolders.setIncomingTextConfig(MagicIncomingTextMessageViewHolder.class, R.layout.item_custom_incoming_text_message);
            messageHolders.setOutcomingTextConfig(MagicOutcomingTextMessageViewHolder.class, R.layout.item_custom_outcoming_text_message);

            messageHolders.setIncomingImageConfig(MagicPreviewMessageViewHolder.class, R.layout.item_custom_incoming_preview_message);
            messageHolders.setOutcomingImageConfig(MagicPreviewMessageViewHolder.class, R.layout.item_custom_outcoming_preview_message);

            messageHolders.registerContentType(CONTENT_TYPE_SYSTEM_MESSAGE, MagicSystemMessageViewHolder.class,
                    R.layout.item_system_message, MagicSystemMessageViewHolder.class, R.layout.item_system_message,
                    this);

            adapter = new MessagesListAdapter<>(conversationUser.getUserId(), messageHolders, new ImageLoader() {
                @Override
                public void loadImage(SimpleDraweeView imageView, @Nullable String url, @Nullable Object payload) {
                    DraweeController draweeController = Fresco.newDraweeControllerBuilder()
                            .setImageRequest(DisplayUtils.getImageRequestForUrl(url, conversationUser))
                            .setControllerListener(DisplayUtils.getImageControllerListener(imageView))
                            .setOldController(imageView.getController())
                            .setAutoPlayAnimations(true)
                            .build();
                    imageView.setController(draweeController);
                }
/*
                @Override
                public void loadImage(SimpleDraweeView imageView, String url) {
                    DraweeController draweeController = Fresco.newDraweeControllerBuilder()
                            .setImageRequest(DisplayUtils.getImageRequestForUrl(url, conversationUser))
                            .setControllerListener(DisplayUtils.getImageControllerListener(imageView))
                            .setOldController(imageView.getController())
                            .setAutoPlayAnimations(true)
                            .build();
                    imageView.setController(draweeController);
                }*/
            });
        } else {
            messagesListView.setVisibility(View.VISIBLE);
        }


        messagesListView.setAdapter(adapter);
        adapter.setLoadMoreListener(this);
        adapter.setDateHeadersFormatter(this::format);
        adapter.setOnMessageLongClickListener(this);

        layoutManager = (LinearLayoutManager) messagesListView.getLayoutManager();

        popupBubble.setRecyclerView(messagesListView);

        popupBubble.setPopupBubbleListener(context -> {
            if (newMessagesCount != 0) {
                int scrollPosition;
                if (newMessagesCount - 1 < 0) {
                    scrollPosition = 0;
                } else {
                    scrollPosition = newMessagesCount - 1;
                }
                new Handler().postDelayed(() -> messagesListView.smoothScrollToPosition(scrollPosition), 200);
            }
        });

        messagesListView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);

                if (newState == AbsListView.OnScrollListener.SCROLL_STATE_IDLE) {
                    if (newMessagesCount != 0) {
                        if (layoutManager.findFirstCompletelyVisibleItemPosition() < newMessagesCount) {
                            newMessagesCount = 0;

                            if (popupBubble != null && popupBubble.isShown()) {
                                popupBubble.hide();
                            }
                        }
                    }
                }
            }
        });


        InputFilter[] filters = new InputFilter[1];
        int lengthFilter = conversationUser.getMessageMaxLength();


        filters[0] = new InputFilter.LengthFilter(lengthFilter);
        messageInput.setFilters(filters);

        messageInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() >= lengthFilter) {
                    messageInput.setError(String.format(Objects.requireNonNull(getResources()).getString(R.string.nc_limit_hit), Integer.toString(lengthFilter)));
                } else {
                    messageInput.setError(null);
                }

                Editable editable = messageInput.getEditableText();
                Spans.MentionChipSpan[] mentionSpans = editable.getSpans(0, messageInput.length(),
                        Spans.MentionChipSpan.class);
                Spans.MentionChipSpan mentionSpan;
                for (int i = 0; i < mentionSpans.length; i++) {
                    mentionSpan = mentionSpans[i];
                    if (start >= editable.getSpanStart(mentionSpan) && start < editable.getSpanEnd(mentionSpan)) {
                        if (!editable.subSequence(editable.getSpanStart(mentionSpan),
                                editable.getSpanEnd(mentionSpan)).toString().trim().equals(mentionSpan.getLabel())) {
                            editable.removeSpan(mentionSpan);
                        }
                    }
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        messageInputView.setAttachmentsListener(new MessageInput.AttachmentsListener() {
            @Override
            public void onAddAttachments() {
                showBrowserScreen(BrowserController.BrowserType.DAV_BROWSER);
            }
        });

        messageInputView.getButton().setOnClickListener(v -> submitMessage());
        messageInputView.getButton().setContentDescription(getResources()
                .getString(R.string.nc_description_send_message_button));

        if (!conversationUser.getUserId().equals("?") && conversationUser.hasSpreedFeatureCapability("mention-flag") && getActivity() != null) {
            getActivity().findViewById(R.id.toolbar).setOnClickListener(v -> showConversationInfoScreen());
        }

        if (currentConversation != null) {
            checkLobbyState(false);
        }
        iceServers = new ArrayList<>();
        if (adapterWasNull) {
            // we're starting
            if (TextUtils.isEmpty(roomToken)) {
                handleFromNotification();
            } else if (TextUtils.isEmpty(conversationName)) {
                getRoomInfo();
            } else {
                setupMentionAutocomplete();
//                19-9 Commented to avoid unnecessary metwork calls
//                joinRoomWithPassword();
            }
        }
    }


    private void checkReadOnlyState() {
        if (currentConversation != null) {
            if (currentConversation.shouldShowLobby(conversationUser) || (currentConversation.getConversationReadOnlyState() != null && currentConversation.getConversationReadOnlyState().equals(Conversation.ConversationReadOnlyState.CONVERSATION_READ_ONLY))) {

                conversationVoiceCallMenuItem.getIcon().setAlpha(99);
                conversationVideoMenuItem.getIcon().setAlpha(99);
                if (messageInputView != null) {
                    messageInputView.setVisibility(View.GONE);
                }

            } else {
                if (conversationVoiceCallMenuItem != null) {
                    conversationVoiceCallMenuItem.getIcon().setAlpha(255);
                }

                if (conversationVideoMenuItem != null) {
                    conversationVideoMenuItem.getIcon().setAlpha(255);
                }

                if (messageInputView != null) {
                    if (currentConversation.shouldShowLobby(conversationUser)) {
                        messageInputView.setVisibility(View.GONE);
                    } else {
                        messageInputView.setVisibility(View.VISIBLE);
                    }
                }
            }
        }
    }

    private void checkLobbyState(boolean lobbyStateChanged) {
        if (currentConversation != null) {

            if (!checkingLobbyStatus) {
                getRoomInfo();
            }
//            lobbyStateChanged=true;
//            fetchSignalingSettings();

            if (landingPageResponse.started || currentConversation.participantType.equals(Participant.ParticipantType.MODERATOR) || currentConversation.participantType.equals(Participant.ParticipantType.OWNER)) {
                lobbyView.setVisibility(View.GONE);
                messagesListView.setVisibility(View.VISIBLE);
                messageInput.setVisibility(View.VISIBLE);
                loadingProgressBar.setVisibility(View.GONE);
                /*//19_9 Calling meeting joining api only if meeting started or user is moderator
                joinRoomWithPassword();*/
                if (lobbyStateChanged) {
                    loadingProgressBar.setVisibility(View.VISIBLE);
                    //Maulik: to manage and allow calling of pull chatmessage
//                    inChat=true;
                    if (isFirstMessagesProcessing) {
                        pullChatMessages(0);
                    } else {
                        pullChatMessages(1);
                    }
                }
            } else {
                lobbyView.setVisibility(View.VISIBLE);
                messagesListView.setVisibility(View.GONE);
                messageInputView.setVisibility(View.GONE);
                loadingProgressBar.setVisibility(View.GONE);
                inChat = false;
                feelLobbyData(currentConversation);

            }


           /* if (currentConversation.shouldShowLobby(conversationUser)) {
                lobbyView.setVisibility(View.VISIBLE);
                messagesListView.setVisibility(View.GONE);
                messageInputView.setVisibility(View.GONE);
                loadingProgressBar.setVisibility(View.GONE);

                if (currentConversation.getLobbyTimer() != null && currentConversation.getLobbyTimer() != 0) {
                    conversationLobbyText.setText(String.format(getResources().getString(R.string.nc_lobby_waiting_with_date), DateUtils.INSTANCE.getLocalDateStringFromTimestampForLobby(currentConversation.getLobbyTimer())));
                } else {
                    conversationLobbyText.setText(R.string.nc_lobby_waiting);
                }
            } else {
                lobbyView.setVisibility(View.GONE);
                messagesListView.setVisibility(View.VISIBLE);
                messageInput.setVisibility(View.VISIBLE);
                if (lobbyStateChanged) {
                    loadingProgressBar.setVisibility(View.VISIBLE);
                    if (isFirstMessagesProcessing) {
                        pullChatMessages(0);
                    } else {
                        pullChatMessages(1);
                    }
                }
            }*/
        }
    }

    private void feelLobbyData(Conversation currentConversation) {
        if (currentConversation.getLobbyTimer() != null && currentConversation.getLobbyTimer() != 0) {

            conversationLobbyText.setText(String.format(getResources().getString(R.string.nc_lobby_waiting_with_date), DateUtils.INSTANCE.getLocalDateStringFromTimestampForLobby(currentConversation.getLobbyTimer())));
        } else {
            conversationLobbyText.setText(R.string.nc_lobby_waiting);
        }
        nameTextView.setText("Welcome, " + userUtils.getCurrentUser().getDisplayName());
        meetingIdValueTextView.setText(currentConversation.token);
        meetingTitleValueTextView.setText(currentConversation.name);
        meetingDetailsValueTextView.setText(landingPageResponse.description);
        meetingStartValueTextView.setText(DateUtils.INSTANCE.getDateTimeStringFromTimestamp(landingPageResponse.start, "MM/dd/yyyy hh:mm a", "Australia/Melbourne"));
        meetingEndValueTextView.setText(DateUtils.INSTANCE.getDateTimeStringFromTimestamp(landingPageResponse.end, "MM/dd/yyyy hh:mm a", "Australia/Melbourne"));
        meetingTimeZoneValueTextView.setText("Australia/Melbourne");


    }

    private void showBrowserScreen(BrowserController.BrowserType browserType) {
        Bundle bundle = new Bundle();
        bundle.putParcelable(BundleKeys.INSTANCE.getKEY_BROWSER_TYPE(), Parcels.wrap(browserType));
        bundle.putParcelable(BundleKeys.INSTANCE.getKEY_USER_ENTITY(), Parcels.wrap(conversationUser));
        bundle.putString(BundleKeys.INSTANCE.getKEY_ROOM_TOKEN(), roomToken);
        getRouter().pushController((RouterTransaction.with(new BrowserController(bundle))
                .pushChangeHandler(new VerticalChangeHandler())
                .popChangeHandler(new VerticalChangeHandler())));
    }

    private void showConversationInfoScreen() {
        Bundle bundle = new Bundle();
        bundle.putParcelable(BundleKeys.INSTANCE.getKEY_USER_ENTITY(), conversationUser);
        bundle.putString(BundleKeys.INSTANCE.getKEY_ROOM_TOKEN(), roomToken);
        getRouter().pushController((RouterTransaction.with(new ConversationInfoController(bundle))
                .pushChangeHandler(new HorizontalChangeHandler())
                .popChangeHandler(new HorizontalChangeHandler())
                .tag(ConversationParticipantsController.TAG)));
    }

    private void showParticipantsScreen() {
        Bundle bundle = new Bundle();
        bundle.putParcelable(BundleKeys.INSTANCE.getKEY_USER_ENTITY(), conversationUser);
        bundle.putString(BundleKeys.INSTANCE.getKEY_ROOM_TOKEN(), roomToken);
        getRouter().pushController((RouterTransaction.with(new ConversationParticipantsController(bundle))
                .pushChangeHandler(new HorizontalChangeHandler())
                .popChangeHandler(new HorizontalChangeHandler())
                .tag(ConversationParticipantsController.TAG)));
    }

    private void setupMentionAutocomplete() {
        float elevation = 6f;
        Drawable backgroundDrawable = new ColorDrawable(getResources().getColor(R.color.bg_default));
        AutocompletePresenter<Mention> presenter = new MentionAutocompletePresenter(getApplicationContext(), roomToken);
        AutocompleteCallback<Mention> callback = new MentionAutocompleteCallback(getActivity(),
                conversationUser, messageInput);

        if (mentionAutocomplete == null && messageInput != null) {
            mentionAutocomplete = Autocomplete.<Mention>on(messageInput)
                    .with(elevation)
                    .with(backgroundDrawable)
                    .with(new MagicCharPolicy('@'))
                    .with(presenter)
                    .with(callback)
                    .build();
        }
    }

    @Override
    protected void onAttach(@NonNull View view) {
        super.onAttach(view);
        eventBus.register(this);

        isLeavingForConversation = false;
        ApplicationWideCurrentRoomHolder.getInstance().setCurrentRoomId(roomId);
        ApplicationWideCurrentRoomHolder.getInstance().setCurrentRoomToken(roomId);
        ApplicationWideCurrentRoomHolder.getInstance().setInCall(false);
        ApplicationWideCurrentRoomHolder.getInstance().setUserInRoom(conversationUser);

        isLinkPreviewAllowed = appPreferences.getAreLinkPreviewsAllowed();

        emojiPopup = EmojiPopup.Builder.fromRootView(view).setOnEmojiPopupShownListener(new OnEmojiPopupShownListener() {
            @Override
            public void onEmojiPopupShown() {
                if (getResources() != null) {
                    smileyButton.setColorFilter(getResources().getColor(R.color.colorPrimary),
                            PorterDuff.Mode.SRC_IN);
                }
            }
        }).setOnEmojiPopupDismissListener(new OnEmojiPopupDismissListener() {
            @Override
            public void onEmojiPopupDismiss() {
                if (smileyButton != null) {
                    smileyButton.setColorFilter(getResources().getColor(R.color.emoji_icons),
                            PorterDuff.Mode.SRC_IN);
                }
            }
        }).setOnEmojiClickListener(new OnEmojiClickListener() {
            @Override
            public void onEmojiClick(@NonNull EmojiImageView emoji, @NonNull Emoji imageView) {
                messageInput.getEditableText().append(" ");
            }
        }).build(messageInput);

        if (getActivity() != null) {
            new KeyboardUtils(getActivity(), getView(), false);
        }

        cancelNotificationsForCurrentConversation();

       /* if (inChat) {
            if (wasDetached && conversationUser.hasSpreedFeatureCapability("no-ping")) {
                currentCall = null;
                wasDetached = false;
                joinRoomWithPassword();
            }
        }*/
        if (inChat) {
            if (wasDetached) {
                currentCall = null;
                wasDetached = false;
                joinRoomWithPassword();
            }
        }

    }

    private void cancelNotificationsForCurrentConversation() {
        //Maulik
       /* if (!conversationUser.hasSpreedFeatureCapability("no-ping") && !TextUtils.isEmpty(roomId)) {
            NotificationUtils.cancelExistingNotificationsForRoom(getApplicationContext(), conversationUser, roomId);
        } else if (!TextUtils.isEmpty(roomToken)) {
            NotificationUtils.cancelExistingNotificationsForRoom(getApplicationContext(), conversationUser, roomToken);
        }*/
    }

    @Override
    protected void onDetach(@NonNull View view) {
        super.onDetach(view);
        ApplicationWideCurrentRoomHolder.getInstance().clear();
        eventBus.unregister(this);

        if (conversationUser.hasSpreedFeatureCapability("no-ping")
                && getActivity() != null && !getActivity().isChangingConfigurations() && !isLeavingForConversation) {
            wasDetached = true;
            leaveRoom();
        }
//        20-9 Maulik enabled wasdetached to restart chat after call
        wasDetached = true;

        if (mentionAutocomplete != null && mentionAutocomplete.isPopupShowing()) {
            mentionAutocomplete.dismissPopup();
        }
    }

    @Override
    protected String getTitle() {
        return conversationName;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (getActivity() != null) {
            getActivity().findViewById(R.id.toolbar).setOnClickListener(null);
        }

        adapter = null;
        inChat = false;
    }

    private void dispose() {
        Disposable disposable;
        for (int i = 0; i < disposableList.size(); i++) {
            if (!(disposable = disposableList.get(i)).isDisposed()) {
                disposable.dispose();
            }
        }
    }

    private void startPing() {
       /* if (!conversationUser.hasSpreedFeatureCapability("no-ping")) {
            ncApi.pingCall(credentials, ApiUtils.getUrlForCallPing(conversationUser.getBaseUrl(), roomToken))
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .repeatWhen(observable -> observable.delay(5000, TimeUnit.MILLISECONDS))
                    .takeWhile(observable -> inChat)
                    .retry(3, observable -> inChat)
                    .subscribe(new Observer<GenericOverall>() {
                        @Override
                        public void onSubscribe(Disposable d) {
                            disposableList.add(d);
                        }

                        @Override
                        public void onNext(GenericOverall genericOverall) {

                        }

                        @Override
                        public void onError(Throwable e) {
                        }

                        @Override
                        public void onComplete() {
                        }
                    });
        }*/
    }

    @OnClick(R.id.smileyButton)
    void onSmileyClick() {
        emojiPopup.toggle();
    }

    private void joinRoomWithPassword() {

        if (currentCall == null) {
            String meetingSession = PreferenceHelper.getSharedPreferenceString(NextcloudTalkApplication.Companion.getSharedApplication().getApplicationContext(), "MEETING", "");
            String hostSession = PreferenceHelper.getSharedPreferenceString(NextcloudTalkApplication.Companion.getSharedApplication().getApplicationContext(), "HOST", "");
//            DialogUtils.INSTANCE.showDialog(getActivity(),"DIALOG");

            showDialog();
            ncApi.joinRoom(credentials,
                    ApiUtils.getUrlForSettingMyselfAsActiveParticipant(conversationUser.getBaseUrl(), roomToken), roomPassword)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .retry(3)
                    .subscribe(new Observer<CallOverall>() {
                        @Override
                        public void onSubscribe(Disposable d) {
                            disposableList.add(d);
                        }

                        @Override
                        public void onNext(CallOverall callOverall) {
                            inChat = true;
                            hideDialog();
                            GenericMeta metaData = callOverall.ocs.meta;
                            if (metaData.statusCode == 200) {
//                            DialogUtils.INSTANCE.closeDialog(getActivity(),"DIALOG");
                                currentCall = callOverall.getOcs().getData();
                                ApplicationWideCurrentRoomHolder.getInstance().setSession(currentCall.getSessionId());
                                startPing();
                                checkLobbyState(false);
                                setupWebsocket();
                                if (isFirstMessagesProcessing) {
                                    pullChatMessages(0);
                                } else {
                                    pullChatMessages(1);
                                }

                                if (magicWebSocketInstance != null) {
                                    magicWebSocketInstance.joinRoomWithRoomTokenAndSession(roomToken,
                                            currentCall.getSessionId());
                                }
                                if (startCallFromNotification != null && startCallFromNotification) {
                                    startCallFromNotification = false;
                                    startACall(voiceOnly);
                                }
                            } else {
                                processJoinRoomError(callOverall);
                            }
                        }

                        @Override
                        public void onError(Throwable e) {
//                            DialogUtils.INSTANCE.closeDialog(getActivity(),"DIALOG");
                            hideDialog();
                            Toast.makeText(context, context.getResources().getString(R.string.str_session_already_running), Toast.LENGTH_LONG).show();
                        }

                        @Override
                        public void onComplete() {
                            hideDialog();
//                            DialogUtils.INSTANCE.closeDialog(getActivity(),"DIALOG");
                        }
                    });
        } else {
            inChat = true;
            ApplicationWideCurrentRoomHolder.getInstance().setSession(currentCall.getSessionId());
            if (magicWebSocketInstance != null) {
                magicWebSocketInstance.joinRoomWithRoomTokenAndSession(roomToken,
                        currentCall.getSessionId());
            }
            startPing();
            if (isFirstMessagesProcessing) {
                pullChatMessages(0);
            } else {
                pullChatMessages(1);
            }
        }
    }

    private void processJoinRoomError(CallOverall callOverall) {
        GenericMeta metaData = callOverall.ocs.meta;
        if (metaData.statusCode == 403) {
            Toast.makeText(context, context.getResources().getString(R.string.str_session_already_running), Toast.LENGTH_LONG).show();
        }

    }


    private void leaveRoom() {
        ncApi.leaveRoom(credentials,
                ApiUtils.getUrlForSettingMyselfAsActiveParticipant(conversationUser.getBaseUrl(),
                        roomToken))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<GenericOverall>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        disposableList.add(d);
                    }

                    @Override
                    public void onNext(GenericOverall genericOverall) {
                        dispose();

                        checkingLobbyStatus = false;

                        if (lobbyTimerHandler != null) {
                            lobbyTimerHandler.removeCallbacksAndMessages(null);
                        }

                        if (magicWebSocketInstance != null && currentCall != null) {
                            magicWebSocketInstance.joinRoomWithRoomTokenAndSession("", currentCall.getSessionId());
                        }

                        if (!isDestroyed() && !isBeingDestroyed() && !wasDetached) {
                            getRouter().popCurrentController();
                        }
                    }

                    @Override
                    public void onError(Throwable e) {
                    }

                    @Override
                    public void onComplete() {
                        dispose();
                    }
                });
    }

    private void setSenderId() {
        try {
            final Field senderId = adapter.getClass().getDeclaredField("senderId");
            senderId.setAccessible(true);
            senderId.set(adapter, conversationUser.getUserId());
        } catch (NoSuchFieldException e) {
            Log.w(TAG, "Failed to set sender id");
        } catch (IllegalAccessException e) {
            Log.w(TAG, "Failed to access and set field");
        }
    }

    private void submitMessage() {
        final Editable editable = messageInput.getEditableText();
        Spans.MentionChipSpan mentionSpans[] = editable.getSpans(0, editable.length(),
                Spans.MentionChipSpan.class);
        Spans.MentionChipSpan mentionSpan;
        for (int i = 0; i < mentionSpans.length; i++) {
            mentionSpan = mentionSpans[i];
            editable.replace(editable.getSpanStart(mentionSpan), editable.getSpanEnd(mentionSpan), "@" + mentionSpan.getId());
        }

        messageInput.setText("");
        sendMessage(editable);
    }

    private void sendMessage(CharSequence message) {

        ncApi.sendChatMessage(credentials, ApiUtils.getUrlForChat(conversationUser.getBaseUrl(), roomToken),
                message, conversationUser.getDisplayName())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<GenericOverall>() {
                    @Override
                    public void onSubscribe(Disposable d) {

                    }

                    @Override
                    public void onNext(GenericOverall genericOverall) {
                        myFirstMessage = message;

                        if (popupBubble != null && popupBubble.isShown()) {
                            popupBubble.hide();
                        }

                        if (messagesListView != null) {
                            messagesListView.smoothScrollToPosition(0);
                        }
                    }

                    @Override
                    public void onError(Throwable e) {
                        if (e instanceof HttpException) {
                            int code = ((HttpException) e).code();
                            if (Integer.toString(code).startsWith("2")) {
                                myFirstMessage = message;

                                if (popupBubble != null && popupBubble.isShown()) {
                                    popupBubble.hide();
                                }

                                messagesListView.smoothScrollToPosition(0);
                            }
                        }
                    }

                    @Override
                    public void onComplete() {

                    }
                });
    }

    private void setupWebsocket() {
        if (magicWebSocketInstance == null) {
            if (WebSocketConnectionHelper.getMagicWebSocketInstanceForUserId(conversationUser.getId()) != null) {
                magicWebSocketInstance = WebSocketConnectionHelper.getMagicWebSocketInstanceForUserId(conversationUser.getId());
            } else {
                magicWebSocketInstance = null;
            }
        }
    }

    private void pullChatMessages(int lookIntoFuture) {
        inChat = true;
        if (!inChat) {
            return;
        }
        isMessageAddedFromSocket = false;
        if (currentConversation.shouldShowLobby(conversationUser)) {
            return;
        }

        if (lookIntoFuture > 0) {
            lookingIntoFuture = true;
        }

        Map<String, Integer> fieldMap = new HashMap<>();
        fieldMap.put("lookIntoFuture", lookIntoFuture);
        fieldMap.put("limit", 25);

        int lastKnown;
        if (lookIntoFuture > 0) {
            lastKnown = globalLastKnownFutureMessageId;
        } else {
            lastKnown = globalLastKnownPastMessageId;
        }

        if (lastKnown != -1) {
            fieldMap.put("lastKnownMessageId", lastKnown);
        }

        showDialog();
        if (!wasDetached) {
            if (lookIntoFuture > 0) {
                ncApi.pullChatMessages(credentials, ApiUtils.getUrlForChat(conversationUser.getBaseUrl(),
                        roomToken),
                        fieldMap)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .takeWhile(observable -> inChat && !wasDetached)
                        .subscribe(new Observer<Response>() {
                            @Override
                            public void onSubscribe(Disposable d) {
                                disposableList.add(d);
                            }

                            @Override
                            public void onNext(Response response) {
                                processMessages(response, true);
                            }

                            @Override
                            public void onError(Throwable e) {
                                if (loadingProgressBar != null) {
                                    loadingProgressBar.setVisibility(View.GONE);
                                }
                                if (globalLastKnownFutureMessageId == -1)
                                    Toast.makeText(context, "Chat not started yet", Toast.LENGTH_LONG).show();

                                hideDialog();

                            }

                            @Override
                            public void onComplete() {
                                if (currentConversation.shouldShowLobby(conversationUser)) {
                                    pullChatMessages(1);
                                }
                                hideDialog();
                            }
                        });

            } else {
                ncApi.pullChatMessages(credentials,
                        ApiUtils.getUrlForChat(conversationUser.getBaseUrl(), roomToken), fieldMap)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .takeWhile(observable -> inChat && !wasDetached)
                        .subscribe(new Observer<Response>() {
                            @Override
                            public void onSubscribe(Disposable d) {
                                disposableList.add(d);
                            }

                            @Override
                            public void onNext(Response response) {
                                processMessages(response, false);
                            }

                            @Override
                            public void onError(Throwable e) {
                                hideDialog();
                                if (isFirstMessagesProcessing) {
                                    emptyLayout.setVisibility(View.VISIBLE);
                                    messagesListView.setVisibility(View.GONE);
                                } else {
                                    emptyLayout.setVisibility(View.GONE);
                                    messagesListView.setVisibility(View.VISIBLE);
                                }
                            }

                            @Override
                            public void onComplete() {
                                hideDialog();
                            }
                        });
            }
        }
    }

    /*When we are fetching messages for the first time, We are setting whole list in adapter and from the next time we are setting each individual message to the list*/
    private void processMessages(Response response, boolean isFromTheFuture) {
        if (isMessageAddedFromSocket) return;
        if (response.code() == 200) {
            ChatOverall chatOverall = (ChatOverall) response.body();
            chatMessageList = chatOverall.getOcs().getData();
            if (loadingProgressBar != null) {
                loadingProgressBar.setVisibility(View.GONE);
            }
            if (isFirstMessagesProcessing) {
                cancelNotificationsForCurrentConversation();

                isFirstMessagesProcessing = false;
                if (loadingProgressBar != null) {
                    loadingProgressBar.setVisibility(View.GONE);
                }

                if (messagesListView != null) {
                    messagesListView.setVisibility(View.VISIBLE);
                }
                if (chatMessageList.size() > 0) {
                    emptyLayout.setVisibility(View.GONE);
                    messagesListView.setVisibility(View.VISIBLE);
                } else {
                    messagesListView.setVisibility(View.GONE);
                    emptyLayout.setVisibility(View.VISIBLE);
                }
            }

            int countGroupedMessages = 0;
            if (!isFromTheFuture) {

                for (int i = 0; i < chatMessageList.size(); i++) {
                    if (chatMessageList.size() > i + 1) {
                        if (TextUtils.isEmpty(chatMessageList.get(i).getSystemMessage()) &&
                                TextUtils.isEmpty(chatMessageList.get(i + 1).getSystemMessage()) &&
                                chatMessageList.get(i + 1).getActorId().equals(chatMessageList.get(i).getActorId()) &&
                                countGroupedMessages < 4 && DateFormatter.isSameDay(chatMessageList.get(i).getCreatedAt(),
                                chatMessageList.get(i + 1).getCreatedAt())) {
                            chatMessageList.get(i).setGrouped(true);
                            countGroupedMessages++;
                        } else {
                            countGroupedMessages = 0;
                        }
                    }

                    ChatMessage chatMessage = chatMessageList.get(i);
                    chatMessage.setLinkPreviewAllowed(isLinkPreviewAllowed);
                    chatMessage.setActiveUser(conversationUser);

                    if (globalLastKnownPastMessageId == -1 || chatMessageList.get(i).getJsonMessageId() <
                            globalLastKnownPastMessageId) {
                        globalLastKnownPastMessageId = chatMessageList.get(i).getJsonMessageId();
                    }

                    if (globalLastKnownFutureMessageId == -1) {
                        if (chatMessageList.get(i).getJsonMessageId() > globalLastKnownFutureMessageId) {
                            globalLastKnownFutureMessageId = chatMessageList.get(i).getJsonMessageId();
                        }
                    }
                }

                if (adapter != null) {
                    adapter.addToEnd(chatMessageList, false);
                }

            } else {

                ChatMessage chatMessage;

                for (int i = 0; i < chatMessageList.size(); i++) {
                    chatMessage = chatMessageList.get(i);

                    chatMessage.setActiveUser(conversationUser);
                    chatMessage.setLinkPreviewAllowed(isLinkPreviewAllowed);

                    // if credentials are empty, we're acting as a guest
                    if (TextUtils.isEmpty(credentials) && myFirstMessage != null && !TextUtils.isEmpty(myFirstMessage.toString())) {
                        if (chatMessage.getActorType().equals("guests")) {
                            conversationUser.setUserId(chatMessage.getActorId());
                            setSenderId();
                        }
                    }

                    boolean shouldScroll = layoutManager.findFirstVisibleItemPosition() == 0 ||
                            (adapter != null && adapter.getItemCount() == 0);

                    if (!shouldScroll && popupBubble != null) {
                        if (!popupBubble.isShown()) {
                            newMessagesCount = 1;
                            popupBubble.show();
                        } else if (popupBubble.isShown()) {
                            newMessagesCount++;
                        }
                    } else {
                        newMessagesCount = 0;
                    }

                    if (adapter != null) {
                        chatMessage.setGrouped(adapter.isPreviousSameAuthor(chatMessage.getActorId(), -1) && (adapter.getSameAuthorLastMessagesCount(chatMessage.getActorId()) % 5) > 0);
                        adapter.addToStart(chatMessage, shouldScroll);
                    }

                }

                String xChatLastGivenHeader;
                if (response.headers().size() > 0 && !TextUtils.isEmpty((xChatLastGivenHeader = response.headers().get
                        ("X-Chat-Last-Given")))) {
                    if (xChatLastGivenHeader != null) {
                        globalLastKnownFutureMessageId = Integer.parseInt(xChatLastGivenHeader);
                    }
                }
            }

            if (!lookingIntoFuture && inChat) {
                pullChatMessages(1);
            }
        } else if (response.code() == 304 && !isFromTheFuture) {
            if (isFirstMessagesProcessing) {
                cancelNotificationsForCurrentConversation();
                messagesListView.setVisibility(View.GONE);
                emptyLayout.setVisibility(View.VISIBLE);

                isFirstMessagesProcessing = false;
                if (loadingProgressBar != null) {
                    loadingProgressBar.setVisibility(View.GONE);
                }
            } else {
                emptyLayout.setVisibility(View.GONE);
                messagesListView.setVisibility(View.VISIBLE);
            }

            historyRead = true;

            if (!lookingIntoFuture && inChat) {
                pullChatMessages(1);
            }
        }
    }

    private void processMessageFromSocket(Map<String, Object> messageMap1, boolean isFromTheFuture) {
        isMessageAddedFromSocket = true;
        if(chatMessageList==null)
            chatMessageList=new ArrayList<>();
        ChatMessage chatMessage = new ChatMessage();
        Map<String, Object> messageMap = (Map<String, Object>) messageMap1.get("comment");
        chatMessage.actorType = messageMap.get("actorType").toString();
        chatMessage.actorId = messageMap.get("actorId").toString();
        chatMessage.actorDisplayName = messageMap.get("actorDisplayName").toString();
        chatMessage.jsonMessageId = Integer.parseInt(String.valueOf(messageMap.get("id")));
        chatMessage.message = (String) messageMap.get("message");
        chatMessage.token = (String) messageMap.get("token");
        chatMessage.timestamp = (long) (messageMap.get("timestamp"));
        chatMessage.activeUser = conversationUser;
//              chatMessage.messageParameters= (HashMap<String, HashMap<String, String>>) messageMap.get("messageParameters");


        boolean shouldScroll = layoutManager.findFirstVisibleItemPosition() == 0 ||
                (adapter != null && adapter.getItemCount() == 0);

        if (!shouldScroll && popupBubble != null) {
            if (!popupBubble.isShown()) {
                newMessagesCount = 1;
                popupBubble.show();
            } else if (popupBubble.isShown()) {
                newMessagesCount++;
            }
        } else {
            newMessagesCount = 0;
        }
        chatMessageList.add(0, chatMessage);
        if (adapter != null) {
            chatMessage.setGrouped(adapter.isPreviousSameAuthor(chatMessage.getActorId(), -1) && (adapter.getSameAuthorLastMessagesCount(chatMessage.getActorId()) % 5) > 0);

            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    adapter.addToStart(chatMessage, shouldScroll);
                    if(messagesListView.getVisibility()==View.GONE) {
                        emptyLayout.setVisibility(View.GONE);
                        messagesListView.setVisibility(View.VISIBLE);
                    }
                }
            });


        }

        globalLastKnownFutureMessageId = chatMessage.jsonMessageId;

    }


    @Override
    public void onLoadMore(int page, int totalItemsCount) {
        if (!historyRead && inChat) {
            pullChatMessages(0);
        }
    }


    @Override
    public String format(Date date) {
        if (DateFormatter.isToday(date)) {
            return getResources().getString(R.string.nc_date_header_today);
        } else if (DateFormatter.isYesterday(date)) {
            return getResources().getString(R.string.nc_date_header_yesterday);
        } else {
            return DateFormatter.format(date, DateFormatter.Template.STRING_DAY_MONTH_YEAR);
        }
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.menu_conversation_participants, menu);
        if (conversationUser.getUserId().equals("?")) {
            menu.removeItem(R.id.conversation_info);
        } else {
            conversationInfoMenuItem = menu.findItem(R.id.conversation_info);
            conversationVoiceCallMenuItem = menu.findItem(R.id.conversation_voice_call);
            conversationVideoMenuItem = menu.findItem(R.id.conversation_video_call);
        }
    }

    @Override
    public void onPrepareOptionsMenu(@NonNull Menu menu) {
        super.onPrepareOptionsMenu(menu);
        if (conversationUser.hasSpreedFeatureCapability("read-only-rooms")) {
            checkReadOnlyState();
        }
    }


    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                getRouter().popCurrentController();
                return true;
            case R.id.conversation_video_call:
                if (conversationVideoMenuItem.getIcon().getAlpha() == 255) {
                    startACall(false);
                    return true;
                }
                return false;
            case R.id.conversation_voice_call:
                if (conversationVoiceCallMenuItem.getIcon().getAlpha() == 255) {
                    startACall(true);
                    return true;
                }
                return false;
            case R.id.conversation_info:
                showConversationInfoScreen();
                return true;
            case R.id.participants:
                showParticipantsScreen();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void startACall(boolean isVoiceOnlyCall) {
        isLeavingForConversation = true;
        if (!isVoiceOnlyCall) {
            Intent videoCallIntent = getIntentForCall(false);
            if (videoCallIntent != null) {
                startActivity(videoCallIntent);
            }
        } else {
            Intent voiceCallIntent = getIntentForCall(true);
            if (voiceCallIntent != null) {
                startActivity(voiceCallIntent);
            }
        }
    }

    private Intent getIntentForCall(boolean isVoiceOnlyCall) {
        if (currentConversation != null) {
            Bundle bundle = new Bundle();
            bundle.putString(BundleKeys.INSTANCE.getKEY_ROOM_TOKEN(), roomToken);
            bundle.putString(BundleKeys.INSTANCE.getKEY_ROOM_ID(), roomId);
            bundle.putParcelable(BundleKeys.INSTANCE.getKEY_USER_ENTITY(), conversationUser);
            bundle.putString(BundleKeys.INSTANCE.getKEY_CONVERSATION_PASSWORD(), roomPassword);
            bundle.putString(BundleKeys.INSTANCE.getKEY_MODIFIED_BASE_URL(), conversationUser.getBaseUrl());

            if (isVoiceOnlyCall) {
                bundle.putBoolean(BundleKeys.INSTANCE.getKEY_CALL_VOICE_ONLY(), true);
            }

            if (getActivity() != null) {
                Intent callIntent = new Intent(getActivity(), MagicCallActivity.class);
                callIntent.putExtras(bundle);

                return callIntent;
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    @Override
    public void onMessageLongClick(IMessage message) {
        if (getActivity() != null) {
            ClipboardManager clipboardManager = (ClipboardManager)
                    getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clipData = ClipData.newPlainText(
                    getResources().getString(R.string.nc_app_name), message.getText());
            if (clipboardManager != null) {
                clipboardManager.setPrimaryClip(clipData);
            }
        }
    }

    @Override
    public boolean hasContentFor(IMessage message, byte type) {
        switch (type) {
            case CONTENT_TYPE_SYSTEM_MESSAGE:
                return !TextUtils.isEmpty(message.getSystemMessage());
        }

        return false;
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    public void onEventFromSocket(WebSocketCommunicationEvent webSocketCommunicationEvent) {
        switch (webSocketCommunicationEvent.type) {
            case "refreshChat":

                if (webSocketCommunicationEvent.hashMap.get(BundleKeys.INSTANCE.getKEY_INTERNAL_USER_ID()).equals(Long.toString(conversationUser.getId()))) {
                    if (roomToken.equals(webSocketCommunicationEvent.hashMap.get(BundleKeys.INSTANCE.getKEY_ROOM_TOKEN()))) {
                        pullChatMessages(2);
                    }
                }
                break;
            default:
                break;
        }
    }


    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    public void onMessageEvent(WebSocketMessageEvent webSocketCommunicationEvent) {
        switch (webSocketCommunicationEvent.type) {

            case "comment":
                processMessageFromSocket(webSocketCommunicationEvent.messageHashMap, true);
//                if (webSocketCommunicationEvent.hashMap.get(BundleKeys.INSTANCE.getKEY_INTERNAL_USER_ID()).equals(Long.toString(conversationUser.getId()))) {
//                    if (roomToken.equals(webSocketCommunicationEvent.hashMap.get(BundleKeys.INSTANCE.getKEY_ROOM_TOKEN()))) {
//                        pullChatMessages(2);

//                    }
//                }
                break;
            default:
                break;
        }
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    public void onMessageEvent(UserMentionClickEvent userMentionClickEvent) {
        if ((!currentConversation.type.equals(Conversation.ConversationType.ROOM_TYPE_ONE_TO_ONE_CALL) || !currentConversation.name.equals(userMentionClickEvent.getUserId()))) {
            RetrofitBucket retrofitBucket =
                    ApiUtils.getRetrofitBucketForCreateRoom(conversationUser.getBaseUrl(), "1",
                            userMentionClickEvent.getUserId(), null);

            ncApi.createRoom(credentials,
                    retrofitBucket.url, retrofitBucket.queryMap)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Observer<RoomOverall>() {
                        @Override
                        public void onSubscribe(Disposable d) {

                        }

                        @Override
                        public void onNext(RoomOverall roomOverall) {
                            Intent conversationIntent = new Intent(getActivity(), MagicCallActivity.class);
                            Bundle bundle = new Bundle();
                            bundle.putParcelable(BundleKeys.INSTANCE.getKEY_USER_ENTITY(), conversationUser);
                            bundle.putString(BundleKeys.INSTANCE.getKEY_ROOM_TOKEN(), roomOverall.ocs.data.token);
                            bundle.putString(BundleKeys.INSTANCE.getKEY_ROOM_ID(), roomOverall.ocs.data.roomId);

                            if (conversationUser.hasSpreedFeatureCapability("chat-v2")) {
                                bundle.putParcelable(BundleKeys.INSTANCE.getKEY_ACTIVE_CONVERSATION(),
                                        Parcels.wrap(roomOverall.ocs.data));
                                conversationIntent.putExtras(bundle);

                                ConductorRemapping.INSTANCE.remapChatController(getRouter(), conversationUser.getId(),
                                        roomOverall.ocs.data.token, bundle, false);

                            } else {
                                conversationIntent.putExtras(bundle);
                                startActivity(conversationIntent);
                                new Handler().postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        if (!isDestroyed() && !isBeingDestroyed()) {
                                            getRouter().popCurrentController();
                                        }
                                    }
                                }, 100);
                            }
                        }

                        @Override
                        public void onError(Throwable e) {

                        }

                        @Override
                        public void onComplete() {
                        }
                    });
        }
    }


}