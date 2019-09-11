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

package com.nextcloud.talk.utils

import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*


object DateUtils {

    fun getLocalDateTimeStringFromTimestamp(timestamp: Long): String {
        val cal = Calendar.getInstance()
        val tz = cal.timeZone

        /* date formatter in local timezone */
        val format = DateFormat.getDateTimeInstance(DateFormat.DEFAULT, DateFormat.MEDIUM, Locale
                .getDefault())
        format.timeZone = tz

        return format.format(Date(timestamp))
    }

    fun getLocalDateStringFromTimestampForLobby(timestamp: Long): String {
        return getLocalDateTimeStringFromTimestamp(timestamp * 1000);
    }

    fun getDateTimeStringFromTimestamp(timestamp: Long,dateFormat: String,timeZone: String): String {
        val cal = Calendar.getInstance()
        val tz = cal.timeZone

        val sdf = java.text.SimpleDateFormat(dateFormat)
        sdf.timeZone=tz;
        val date = java.util.Date(timestamp * 1000)
        return sdf.format(date)

    }
}
