package com.myai.assistant

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.view.Gravity
import android.view.ViewGroup
import android.widget.*
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

class MainActivity : Activity() {
    private val prefs by lazy { getSharedPreferences("assistant", MODE_PRIVATE) }
    private lateinit var chat: LinearLayout
    private lateinit var input: EditText
    private lateinit var key: EditText
    private val history = JSONArray()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(18,18,18,12); setBackgroundColor(Color.rgb(16,19,27)) }
        val title = TextView(this).apply { text = "AI Assistant"; textSize = 25f; setTextColor(Color.WHITE); gravity = Gravity.CENTER; setPadding(0,12,0,18) }
        root.addView(title)
        key = EditText(this).apply { hint = "Anthropic API key"; setSingleLine(); setTextColor(Color.WHITE); setHintTextColor(0xff9aa4b2.toInt()); setText(prefs.getString("key","")); setBackgroundColor(0xff252c38.toInt()) }
        root.addView(key, LinearLayout.LayoutParams(-1,54))
        val save = Button(this).apply { text = "Save API key"; setOnClickListener { prefs.edit().putString("key",key.text.toString().trim()).apply(); toast("Saved on this device") } }
        root.addView(save)
        val scroll = ScrollView(this)
        chat = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(4,10,4,10) }
        scroll.addView(chat)
        root.addView(scroll, LinearLayout.LayoutParams(-1,0,1f))
        input = EditText(this).apply { hint = "Write a message..."; setTextColor(Color.WHITE); setHintTextColor(0xff9aa4b2.toInt()); setBackgroundColor(0xff252c38.toInt()) }
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        row.addView(input, LinearLayout.LayoutParams(0,56,1f))
        row.addView(Button(this).apply { text = "Send"; setOnClickListener { send() } })
        root.addView(row)
        setContentView(root)
        bubble("مرحباً! احفظ مفتاح Anthropic API ثم اكتب رسالتك.", false)
    }

    private fun send() {
        val message = input.text.toString().trim()
        if (message.isEmpty()) return
        val apiKey = key.text.toString().trim()
        if (apiKey.isEmpty()) { toast("Enter your Anthropic API key first"); return }
        input.setText("")
        history.put(JSONObject().put("role","user").put("content",message))
        bubble(message,true)
        bubble("Thinking...",false)
        thread {
            try {
                val conn = (URL("https://api.anthropic.com/v1/messages").openConnection() as HttpURLConnection)
                conn.requestMethod = "POST"; conn.connectTimeout = 20000; conn.readTimeout = 60000
                conn.doOutput = true
                conn.setRequestProperty("content-type","application/json")
                conn.setRequestProperty("x-api-key",apiKey)
                conn.setRequestProperty("anthropic-version","2023-06-01")
                val body = JSONObject().put("model","claude-3-5-haiku-20241022").put("max_tokens",1200).put("messages",history)
                conn.outputStream.use { it.write(body.toString().toByteArray()) }
                val stream = if (conn.responseCode in 200..299) conn.inputStream else conn.errorStream
                val response = stream.bufferedReader().use { it.readText() }
                if (conn.responseCode !in 200..299) throw Exception(JSONObject(response).optString("error",response))
                val answer = JSONObject(response).getJSONArray("content").getJSONObject(0).getString("text")
                history.put(JSONObject().put("role","assistant").put("content",answer))
                runOnUiThread { chat.removeViewAt(chat.childCount-1); bubble(answer,false) }
            } catch (e: Exception) {
                runOnUiThread { chat.removeViewAt(chat.childCount-1); bubble("Error: ${e.message}",false) }
            }
        }
    }

    private fun bubble(text: String, user: Boolean) {
        val view = TextView(this).apply {
            this.text = text; textSize = 16f; setTextColor(Color.WHITE); setPadding(14,12,14,12)
            setBackgroundColor(if(user) 0xff285b87.toInt() else 0xff292f3b.toInt())
        }
        val lp = LinearLayout.LayoutParams((resources.displayMetrics.widthPixels*0.88).toInt(),ViewGroup.LayoutParams.WRAP_CONTENT)
        lp.gravity = if(user) Gravity.END else Gravity.START; lp.setMargins(0,6,0,6)
        chat.addView(view,lp)
    }
    private fun toast(s:String) = Toast.makeText(this,s,Toast.LENGTH_SHORT).show()
}
