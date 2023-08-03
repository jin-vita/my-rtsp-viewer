package org.techtown.rtspviewer

import android.util.Log
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.*

class AppData {

    companion object {
        /**
         * DEBUG 로그를 찍을 것인지 여부 확인
         */
        var isDebug = true

        /**
         * ERROR 로그를 찍을 것인지 여부 확인
         */
        var isError = true

        /**
         * DEBUG 로그 찍기
         *
         * @param tag
         * @param msg
         */
        fun debug(tag: String, msg: String) {
            if (isDebug) Log.d(tag, msg)
        }

        /**
         * ERROR 로그 찍기
         *
         * @param tag
         * @param msg
         */
        fun error(tag: String, msg: String) {
            if (isError) Log.e(tag, msg)
        }

        /**
         * Show Toast message during 1 second
         *
         * @param msg 내용
         */
        fun showToast(msg: String) {
            if (Companion::toast.isInitialized) toast.cancel()
            toast = Toast.makeText(MyApp.getContext(), msg, Toast.LENGTH_SHORT)
            toast.show()
        }

        private lateinit var toast: Toast

        //        const val BASE_URL = "https://119.6.3.91:40018/moms/v1/"
        const val BASE_URL = "https://119.6.3.91:40023/moms/v1/rcs/ltr/"
//        const val BASE_URL = "http://192.168.43.234:8001/moms/v1/rcs/ltr/"

        private var seqCode = 0

        /**
         * 요청 코드 생성
         */
        fun generateRequestCode(): String {
            val format = SimpleDateFormat("yyyyMMddHHmmssSSS", Locale.KOREAN)
            seqCode += 1
            if (seqCode > 999) seqCode = 1

            var seqCodeStr = seqCode.toString()
            if (seqCodeStr.length == 1) {
                seqCodeStr = "00$seqCodeStr"
            } else if (seqCodeStr.length == 2) {
                seqCodeStr = "0$seqCodeStr"
            }

            val date = Date()
            val dateStr = format.format(date)

            return dateStr + seqCodeStr
        }

    }

}