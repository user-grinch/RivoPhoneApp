package com.grinch.rivo4.controller

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class CallActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        when (intent?.action) {
            ACTION_ANSWER_CALL -> CallService.answerCall()
            ACTION_DECLINE_CALL -> CallService.declineCall()
            ACTION_TOGGLE_MUTE -> CallService.toggleMute()
            ACTION_TOGGLE_SPEAKER -> CallService.cycleAudioRoute()
        }
    }

    companion object {
        const val ACTION_ANSWER_CALL = "com.grinch.rivo4.ACTION_ANSWER_CALL"
        const val ACTION_DECLINE_CALL = "com.grinch.rivo4.ACTION_DECLINE_CALL"
        const val ACTION_TOGGLE_MUTE = "com.grinch.rivo4.ACTION_TOGGLE_MUTE"
        const val ACTION_TOGGLE_SPEAKER = "com.grinch.rivo4.ACTION_TOGGLE_SPEAKER"
    }
}
