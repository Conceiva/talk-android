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

package com.nextcloud.talk.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.hussain_chachuliya.gifdialog.GifDialog
import com.nextcloud.talk.BuildConfig
import com.nextcloud.talk.R

import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Date

object DialogUtils {
     var dialog: GifDialog?=null

    fun showDialog(context: Context,tag: String)
    {
        if(dialog == null) {
            dialog = GifDialog.with(context)

            dialog!!.isCancelable(false)
                    .setText("")
                    .setTextSize(18)
                    .setWidth(300)
                    .setHeight(300)
                    .setTextBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent))
                    .setTextColor(ContextCompat.getColor(context,R.color.colorPrimary))
                    .setResourceId(R.drawable.spinner)
            dialog?.showDialog(tag)
        }
    }

    fun closeDialog(context: Context,tag: String)
    {
        dialog?.dismissDialog(tag)
        dialog=null;
    }
}
