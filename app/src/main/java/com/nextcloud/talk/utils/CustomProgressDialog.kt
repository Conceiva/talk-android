package com.nextcloud.talk.utils

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.Handler

import android.view.LayoutInflater
import android.widget.ProgressBar
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction

import com.nextcloud.talk.R

// default constructor. Needed so rotation doesn't crash
object CustomProgressDialog {

    internal var dialog: AlertDialog? = null

    fun show(mContext: Context) {
        if (dialog!=null && dialog!!.isShowing()!!)
        {return}

        if(dialog==null) {
            val builder = AlertDialog.Builder(mContext)
            builder.setCancelable(false) // if you want user to wait for some process to finish,
            builder.setView(R.layout.dialog_progress)

            dialog = builder.create()
            dialog!!.getWindow()?.setBackgroundDrawable(ColorDrawable(android.graphics.Color.TRANSPARENT));
        }
            dialog!!.show()

        }


    fun hide(mContext: Context) {
        if (dialog != null && dialog!!.isShowing) {
            dialog!!.dismiss()
        }
    }
}
