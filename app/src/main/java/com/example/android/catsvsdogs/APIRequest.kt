package com.example.android.catsvsdogs

import android.os.AsyncTask
import java.io.IOException
import java.net.URL
import java.util.*
import javax.net.ssl.HttpsURLConnection

class APIRequest: AsyncTask<String, Void, String?>() {

    override fun doInBackground(vararg str: String?): String? {
        val catConst = "cat"

        try {
            val url = URL(str[0])
            val connection = url.openConnection() as HttpsURLConnection
            connection.requestMethod = "GET"
            connection.readTimeout = 15000
            connection.connectTimeout = 15000

            if (str[1] == catConst) {
                // TODO: Set your API KEY for the Cat API
                connection.setRequestProperty("x-api-key", "YOUR CAT API KEY")
            }
            connection.connect()

            val isr = connection.inputStream
            val sc = Scanner(isr)
            sc.useDelimiter("\\A")
            if (sc.hasNext()) {
                connection.disconnect()
                return sc.next()
            }
            else {
                connection.disconnect()
                return null
            }
        } catch (err: IOException) {
            return null
        }
    }

}