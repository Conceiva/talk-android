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

package com.nextspreed.controllers

import android.content.Context
import android.graphics.drawable.Drawable
import android.graphics.drawable.LayerDrawable
import android.opengl.Visibility
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.appcompat.widget.SwitchCompat
import androidx.core.content.ContextCompat
import androidx.emoji.widget.EmojiTextView
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.work.Data
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import autodagger.AutoInjector
import butterknife.BindView
import butterknife.OnClick
import com.afollestad.materialdialogs.LayoutMode.WRAP_CONTENT
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.bottomsheets.BottomSheet
import com.afollestad.materialdialogs.datetime.dateTimePicker
import com.bluelinelabs.conductor.changehandler.HorizontalChangeHandler
import com.bluelinelabs.logansquare.LoganSquare
import com.facebook.drawee.backends.pipeline.Fresco
import com.facebook.drawee.view.SimpleDraweeView
import com.nextcloud.talk.R
import com.nextcloud.talk.adapters.items.UserItem
import com.nextcloud.talk.api.NcApi
import com.nextcloud.talk.application.NextcloudTalkApplication
import com.nextcloud.talk.controllers.CallControllerSpreed
import com.nextcloud.talk.controllers.base.BaseController
import com.nextcloud.talk.events.PeerConnectionEvent
import com.nextcloud.talk.events.WebSocketCommunicationEvent
import com.nextcloud.talk.jobs.DeleteConversationWorker
import com.nextcloud.talk.jobs.LeaveConversationWorker
import com.nextcloud.talk.models.ExternalSignalingServer
import com.nextcloud.talk.models.database.UserEntity
import com.nextcloud.talk.models.json.conversations.Conversation
import com.nextcloud.talk.models.json.conversations.RoomOverall
import com.nextcloud.talk.models.json.converters.EnumNotificationLevelConverter
import com.nextcloud.talk.models.json.generic.GenericOverall
import com.nextcloud.talk.models.json.participants.Participant
import com.nextcloud.talk.models.json.participants.ParticipantsOverall
import com.nextcloud.talk.models.json.signaling.NCSignalingMessage
import com.nextcloud.talk.utils.ApiUtils
import com.nextcloud.talk.utils.DateUtils
import com.nextcloud.talk.utils.DisplayUtils
import com.nextcloud.talk.utils.bundle.BundleKeys
import com.nextcloud.talk.utils.preferences.preferencestorage.DatabaseStorageModule
import com.nextcloud.talk.utils.singletons.ApplicationWideCurrentRoomHolder
import com.nextcloud.talk.utils.singletons.ApplicationWideCurrentRoomHolder.getInstance
import com.nextspreed.adapters.messages.items.ParticipantItem
import com.nextspreed.events.UserAddRemoveEvent
import com.nextspreed.utils.ApplicationWideConstants
import com.yarolegovich.lovelydialog.LovelySaveStateHandler
import com.yarolegovich.lovelydialog.LovelyStandardDialog
import com.yarolegovich.mp.*
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.common.SmoothScrollLinearLayoutManager
import eu.davidea.flexibleadapter.items.AbstractFlexibleItem
import io.reactivex.Observer
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import me.zhanghai.android.effortlesspermissions.EffortlessPermissions
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.webrtc.SurfaceViewRenderer
import java.util.*
import javax.inject.Inject
import kotlin.collections.ArrayList
import kotlin.collections.HashMap


@AutoInjector(NextcloudTalkApplication::class)
class ConversationParticipantsController(args: Bundle) : BaseController(args) {


    @BindView(R.id.participantsTextView)
    lateinit var participantsTextView: TextView

    @BindView(R.id.recycler_view)
    lateinit var recyclerView: RecyclerView

    @BindView(R.id.noDataTextView)
    lateinit var noDataTextView: TextView

    @BindView(R.id.progressBar)
    lateinit var progressBar: ProgressBar

    @BindView(R.id.swipeRefreshLayoutView)
    lateinit var swipeRefreshLayoutView: SwipeRefreshLayout

    @BindView(R.id.emptyLayout)
    lateinit var emptyLayout: RelativeLayout

    @set:Inject
    lateinit var ncApi: NcApi
    @set:Inject
    lateinit var context: Context

    @Inject
    lateinit var eventBus: EventBus

    private val conversationToken: String?
    private val conversationUser: UserEntity?
    private val credentials: String?
    private var roomDisposable: Disposable? = null
    private var participantsDisposable: Disposable? = null

    private var databaseStorageModule: DatabaseStorageModule? = null
    private var conversation: Conversation? = null

    private var adapter: FlexibleAdapter<AbstractFlexibleItem<*>>? = null
    private var recyclerViewItems: MutableList<AbstractFlexibleItem<*>> = ArrayList()

    private var saveStateHandler: LovelySaveStateHandler? = null

    var participantList: ArrayList<Participant> = ArrayList<Participant>()

    private val workerData: Data?
        get() {
            if (!TextUtils.isEmpty(conversationToken) && conversationUser != null) {
                val data = Data.Builder()
                data.putString(BundleKeys.KEY_ROOM_TOKEN, conversationToken)
                data.putLong(BundleKeys.KEY_INTERNAL_USER_ID, conversationUser.id)
                return data.build()
            }

            return null
        }

    init {
        setHasOptionsMenu(true)
        NextcloudTalkApplication.sharedApplication?.componentApplication?.inject(this)
        conversationUser = args.getParcelable(BundleKeys.KEY_USER_ENTITY)
        conversationToken = args.getString(BundleKeys.KEY_ROOM_TOKEN)
        credentials = ApiUtils.getCredentials(conversationUser!!.username, conversationUser.token)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                router.popCurrentController()
                return true
            }
            else -> return super.onOptionsItemSelected(item)
        }
    }

    override fun inflateView(inflater: LayoutInflater, container: ViewGroup): View {
        return inflater.inflate(R.layout.controller_participants_rv, container, false)
    }

    override fun onAttach(view: View) {
        super.onAttach(view)
        if (databaseStorageModule == null) {
            databaseStorageModule = DatabaseStorageModule(conversationUser!!, conversationToken)
        }
        eventBus.register(this)
//        actionBar!!.setDisplayHomeAsUpEnabled(true)
    }

    override fun onDetach(view: View) {
        super.onDetach(view)
        eventBus.unregister(this)
    }

    override fun onViewBound(view: View) {
        super.onViewBound(view)

        if (saveStateHandler == null) {
            saveStateHandler = LovelySaveStateHandler()
        }
//        actionBar!!.setDisplayHomeAsUpEnabled(true)
        if (adapter == null) {
            getListOfParticipants()
//            generateParticipantsList(ApplicationWideCurrentRoomHolder.getInstance().participantsList);
        } else {
            setupAdapter()
        }
    }


    private fun showLovelyDialog(dialogId: Int, savedInstanceState: Bundle) {
        when (dialogId) {
//            ID_DELETE_CONVERSATION_DIALOG -> showDeleteConversationDialog(savedInstanceState)
            else -> {
            }
        }
    }


    override fun onSaveViewState(view: View, outState: Bundle) {
        saveStateHandler!!.saveInstanceState(outState)
        super.onSaveViewState(view, outState)
    }

    override fun onRestoreViewState(view: View, savedViewState: Bundle) {
        super.onRestoreViewState(view, savedViewState)
        if (LovelySaveStateHandler.wasDialogOnScreen(savedViewState)) {
            //Dialog won't be restarted automatically, so we need to call this method.
            //Each dialog knows how to restore its state
            showLovelyDialog(LovelySaveStateHandler.getSavedDialogId(savedViewState), savedViewState)
        }
    }

    private fun setupAdapter() {
        if (activity != null) {
//            if (adapter == null) {
                adapter = FlexibleAdapter(recyclerViewItems, activity, true)
//            }

            val layoutManager = SmoothScrollLinearLayoutManager(activity)
            recyclerView?.layoutManager = layoutManager
            recyclerView?.setHasFixedSize(true)

            var divider = DividerItemDecoration(recyclerView.getContext(), DividerItemDecoration.VERTICAL);
            divider.setDrawable(context.resources.getDrawable(R.drawable.recyclerview_divider))
            recyclerView.addItemDecoration(divider);
            recyclerView?.setItemAnimator(null);
            recyclerView?.adapter = adapter
            if (recyclerViewItems.count() > 0) {
                manageRecyclerViewVisibility(true, "")
            } else {
                manageRecyclerViewVisibility(false, context.resources.getString(R.string.err_no_participants))
            }
        }
    }

    private fun handleParticipants(participants: List<Participant>) {
        var userItem: ParticipantItem
        var participant: Participant
        progressBar.visibility = View.GONE

        recyclerViewItems.clear()
        var ownUserItem: ParticipantItem? = null

        for (i in participants.indices) {
            participant = participants[i]
            userItem = ParticipantItem(participant, conversationUser, null)
            userItem.isEnabled = participant.sessionId != "0"
            if (!TextUtils.isEmpty(participant.userId) && participant.userId == conversationUser!!.userId) {
                ownUserItem = userItem
                userItem.model.sessionId = "-1"
                userItem.isEnabled = true
                participant.displayName = conversationUser.displayName;
            } else {
                recyclerViewItems.add(userItem)
            }
        }
        participantList = participants as ArrayList<Participant>;

        if (ownUserItem != null) {
            recyclerViewItems.add(0, ownUserItem)
        }

        setupAdapter()

        adapter!!.notifyDataSetChanged()
    }

    private fun handleParticipantsForStatusUpdate(participantToUpdate: HashMap<String, Any>) {
        /*var userItem: ParticipantItem
        var participant: Participant
        progressBar.visibility = View.GONE

        recyclerViewItems = ArrayList()
        var ownUserItem: ParticipantItem? = null

        for (i in participantList.indices) {
            participant = participantList[i]
            userItem = ParticipantItem(participant, conversationUser, null)
            userItem.isEnabled = participant.sessionId != "0"
            if (participantToUpdate.get("userId").toString().equals(participant.userId)) {
                if (participantToUpdate["Audio"] != null)
                    participant.audioStatus = participantToUpdate["Audio"] as Participant.AudioFlags?
                if (participantToUpdate["Video"] != null)
                    participant.videoStatus = participantToUpdate["Video"] as Participant.VideoFlags?
            }
            if (!TextUtils.isEmpty(participant.userId) && participant.userId == conversationUser!!.userId) {
                ownUserItem = userItem
                participant.displayName = conversationUser.displayName;
                userItem.model.sessionId = "-1"
                userItem.isEnabled = true
            } else {
                recyclerViewItems.add(userItem)
            }

        }

        if (ownUserItem != null) {
            recyclerViewItems.add(0, ownUserItem)
        }*/

//        setupAdapter()

        var participant: Participant
        for (i in participantList.indices) {
            var participant = participantList[i]
            if (participant.sessionId.equals(participantToUpdate.get(ApplicationWideConstants.SESSION_ID))) {
                var participantItem: ParticipantItem = recyclerViewItems.get(i) as ParticipantItem
                if (participantToUpdate[ApplicationWideConstants.AUDIO] != null)
                    participant.audioStatus = participantToUpdate[ApplicationWideConstants.AUDIO] as Participant.AudioFlags?
                if (participantToUpdate[ApplicationWideConstants.VIDEO] != null)
                    participant.videoStatus = participantToUpdate[ApplicationWideConstants.VIDEO] as Participant.VideoFlags?
                break
            }

        }
        adapter?.notifyDataSetChanged()

    }

    private fun handleParticipantsForNameUpdate(participantToUpdate: HashMap<String, Any>) {
        var userItem: ParticipantItem
        var participant: Participant
        progressBar.visibility = View.GONE

        var ownUserItem: ParticipantItem? = null

        for (i in participantList.indices) {
            participant = participantList[i]
            if (participant.sessionId.equals(participantToUpdate.get(ApplicationWideConstants.SESSION_ID))) {
                var participantItem: ParticipantItem = recyclerViewItems.get(i) as ParticipantItem
                participantItem.getModel().displayName = participantToUpdate.get(ApplicationWideConstants.DISPLAY_NAME).toString()
                break
            }

        }
        adapter?.notifyDataSetChanged()
//        setupAdapter()

    }

    private fun generateParticipantsList(participantToUpdate: List<HashMap<String, Any>>) {

        var userItem: ParticipantItem
        lateinit var participant: Participant;
        recyclerViewItems = ArrayList()
        var ownUserItem: ParticipantItem? = null
        participantList.clear()
        for (i in participantToUpdate.indices) {

            var participantMap: HashMap<String, Any> = participantToUpdate.get(i)
            /*if(participantMap.get(ApplicationWideConstants.IN_CALL) as Boolean)
            {*/
                participant = Participant();
                participant.type = Participant.getParticipantType(participantMap.get(ApplicationWideConstants.PARTICIPANT_TYPE).toString().toInt())
                participant.selected = false
                participant.roomId = conversationToken.toString().toLong()
                participant.audioStatus = Participant.AudioFlags.ENABLED
                participant.videoStatus = Participant.VideoFlags.ENABLED

                if (participantMap.containsKey(ApplicationWideConstants.AUDIO))
                    participant.audioStatus = participantMap[ApplicationWideConstants.AUDIO] as Participant.AudioFlags?

                if (participantMap.containsKey(ApplicationWideConstants.VIDEO))
                    participant.videoStatus = participantMap[ApplicationWideConstants.VIDEO] as Participant.VideoFlags?

                participant.sessionId = participantMap.get(ApplicationWideConstants.SESSION_ID) as String?
                participant.userId = participantMap.get(ApplicationWideConstants.USER_ID) as String?

                if (participantMap.containsKey(ApplicationWideConstants.DISPLAY_NAME))
                    participant.displayName = participantMap.get(ApplicationWideConstants.DISPLAY_NAME) as String?

                participant.lastPing = participantMap.get(ApplicationWideConstants.LAST_PING) as Long
                participant.inCall = participantMap.get(ApplicationWideConstants.IN_CALL) as Boolean

                userItem = ParticipantItem(participant, conversationUser, null)
                userItem.isEnabled = participant.sessionId != "0"

                if (!TextUtils.isEmpty(participant.userId) && participant.userId == conversationUser!!.userId) {
                    ownUserItem = userItem
                    participant.displayName = conversationUser.displayName;
                    userItem.model.sessionId = "-1"
                    userItem.isEnabled = true
                } else {
                    recyclerViewItems.add(userItem)
                    (participantList as ArrayList<Participant>).add(participant)
                }
//            }
        }

        if (ownUserItem != null) {
            (participantList as ArrayList<Participant>).add(0, participant)

            recyclerViewItems.add(0, ownUserItem)
        }

        setupAdapter()

        adapter!!.notifyDataSetChanged()
    }

    override fun getTitle(): String? {
//        actionBar!!.setDisplayHomeAsUpEnabled(true)
        return resources!!.getString(R.string.str_conversation_participants)

    }

    private fun getListOfParticipants() {
        ncApi.getPeersForCall(credentials, ApiUtils.getUrlForParticipants(conversationUser!!.baseUrl, conversationToken))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(object : Observer<ParticipantsOverall> {
                    override fun onSubscribe(d: Disposable) {
                        participantsDisposable = d
                    }

                    override fun onNext(participantsOverall: ParticipantsOverall) {
                        handleParticipants(participantsOverall.ocs.data)
                    }

                    override fun onError(e: Throwable) {
                        progressBar.visibility = View.GONE
                        manageRecyclerViewVisibility(false, context.resources.getString(R.string.err_no_participants))
                    }

                    override fun onComplete() {
                        participantsDisposable!!.dispose()
                        progressBar.visibility = View.GONE
                    }
                })

    }

    private fun manageRecyclerViewVisibility(recyclerViewVisibility: Boolean, message: String) {
        progressBar.visibility = View.GONE
        if (recyclerViewVisibility) {
            emptyLayout.visibility = View.GONE
            swipeRefreshLayoutView.visibility = View.VISIBLE
        } else {
            emptyLayout.visibility = View.VISIBLE
            noDataTextView.setText(message)
            swipeRefreshLayoutView.visibility = View.GONE
        }

    }


    private fun popTwoLastControllers() {
        var backstack = router.backstack
        backstack = backstack.subList(0, backstack.size - 2)
        router.setBackstack(backstack, HorizontalChangeHandler())
    }

    //    UserAddRemoveEvent
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onUserUpdate (userUpdate: UserAddRemoveEvent)
    {
        generateParticipantsList(ApplicationWideCurrentRoomHolder.getInstance().participantsList)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMessageEvent(peerConnectionEvent: PeerConnectionEvent) {
        if (peerConnectionEvent.peerConnectionEventType.equals(PeerConnectionEvent.PeerConnectionEventType
                        .PEER_CLOSED)) {
        } else if (peerConnectionEvent.peerConnectionEventType.equals(PeerConnectionEvent
                        .PeerConnectionEventType.NICK_CHANGE)) {
            gotNick(peerConnectionEvent.sessionId, peerConnectionEvent.nick, true, peerConnectionEvent.videoStreamType)
        } else if (peerConnectionEvent.peerConnectionEventType.equals(PeerConnectionEvent
                        .PeerConnectionEventType.VIDEO_CHANGE)) {
            /* gotAudioOrVideoChange(true, peerConnectionEvent.sessionId + "+" + peerConnectionEvent.videoStreamType,
                     peerConnectionEvent.changeValue)*/
            gotAudioOrVideoChange(true, peerConnectionEvent.sessionId,
                    peerConnectionEvent.changeValue)
        } else if (peerConnectionEvent.peerConnectionEventType.equals(PeerConnectionEvent
                        .PeerConnectionEventType.AUDIO_CHANGE)) {
            /*gotAudioOrVideoChange(false, peerConnectionEvent.sessionId + "+" + peerConnectionEvent.videoStreamType,
                    peerConnectionEvent.changeValue)
*/
            gotAudioOrVideoChange(false, peerConnectionEvent.sessionId,
                    peerConnectionEvent.changeValue)
        }

    }

    private fun gotAudioOrVideoChange(video: Boolean, sessionId: String, change: Boolean) {
        var selectedparticipant: java.util.HashMap<String, Any>? = ApplicationWideCurrentRoomHolder.getInstance().getParticipantForSessionID(sessionId);
        if (!video) {
            if (change) {
                selectedparticipant!!.put(ApplicationWideConstants.AUDIO, Participant.AudioFlags.ENABLED)

            } else {
                selectedparticipant!!.put(ApplicationWideConstants.AUDIO, Participant.AudioFlags.DISABLED)
            }
        } else {
            if (change) {
                selectedparticipant!!.put(ApplicationWideConstants.VIDEO, Participant.VideoFlags.ENABLED)

            } else {
                selectedparticipant!!.put(ApplicationWideConstants.VIDEO, Participant.VideoFlags.DISABLED)
            }
        }
        handleParticipantsForStatusUpdate(selectedparticipant);
    }

    private fun gotNick(sessionOrUserId: String, nick: String, isFromAnEvent: Boolean, type: String) {

        if (!(ApplicationWideCurrentRoomHolder.getInstance().nameSessionIdCombinationMap.containsKey(ApplicationWideConstants.DISPLAY_NAME))) {
            ApplicationWideCurrentRoomHolder.getInstance().nameSessionIdCombinationMap.put(ApplicationWideConstants.DISPLAY_NAME, nick);
            for (i in ApplicationWideCurrentRoomHolder.getInstance().participantsList.indices) {
                var map = ApplicationWideCurrentRoomHolder.getInstance().participantsList[i]
                map.put(ApplicationWideConstants.DISPLAY_NAME, nick)
                if (map.get(ApplicationWideConstants.SESSION_ID).toString().equals(sessionOrUserId) || map.get(ApplicationWideConstants.USER_ID).toString().equals(sessionOrUserId)) {

                    handleParticipantsForNameUpdate(map)
                    return
                }

            }
        }
    }

    private fun processUsersInRoom(users: List<HashMap<String, Any>>) {
        val newSessions = ArrayList<String>()
        val oldSesssions = HashSet<String>()

        for (participant in users) {
            /*if (participant["sessionId"] != callSession)
            {*/
            val inCallObject = participant["inCall"]
            val isNewSession: Boolean
            if (inCallObject is Boolean) {
                isNewSession = inCallObject
            } else {
                isNewSession = inCallObject as Long != 0L
            }

            if (isNewSession) {
                newSessions.add(participant["sessionId"]!!.toString())
            } else {
                oldSesssions.add(participant["sessionId"]!!.toString())
            }

            //}
        }
    }

    companion object {

        const val TAG: String = "ConversationParticipantsController"
        private const val ID_DELETE_CONVERSATION_DIALOG = 0
    }
}
