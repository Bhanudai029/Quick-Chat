package com.example.chattingapp.services

import com.example.chattingapp.model.Message
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.realtime.Realtime

import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.decodeRecord
import io.github.jan.supabase.realtime.realtime
import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.storage.storage
import io.github.jan.supabase.serializer.KotlinXSerializer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.util.Date
import androidx.core.util.Consumer

object SupabaseService {
    // Keys loaded from BuildConfig (local.properties)
    
    lateinit var client: SupabaseClient
    private val scope = CoroutineScope(Dispatchers.IO)

    fun init() {
        if (::client.isInitialized) return
        
        client = createSupabaseClient(
            supabaseUrl = com.example.chattingapp.BuildConfig.SUPABASE_URL,
            supabaseKey = com.example.chattingapp.BuildConfig.SUPABASE_KEY
        ) {
            install(Postgrest)
            install(Realtime)
            install(Storage)
            defaultSerializer = KotlinXSerializer(Json { ignoreUnknownKeys = true })
        }
    }

    // Java-friendly callback interface
    interface MessageCallback {
        fun onMessagesReceived(messages: List<MessageDto>)
        fun onError(error: String)
        fun onNewMessage(message: MessageDto)
    }

    fun fetchGlobalMessages(callback: MessageCallback) {
        scope.launch {
            try {
                val results = client.from("global_messages")
                    .select {
                        order("created_at", Order.ASCENDING)
                        limit(50)
                    }
                    .decodeList<MessageDto>()
                
                callback.onMessagesReceived(results)
            } catch (e: Exception) {
                callback.onError(e.message ?: "Unknown error")
            }
        }
    }

    fun sendGlobalMessage(senderId: String, senderName: String, senderAvatarUrl: String?, text: String, type: String, fileUrl: String?, duration: Long? = null) {
        scope.launch {
            try {
                val msg = MessageDto(
                    sender_id = senderId,
                    sender_name = senderName,
                    sender_avatar_url = senderAvatarUrl,
                    message = text,
                    type = type,
                    file_url = fileUrl,
                    duration = duration
                )
                client.from("global_messages").insert(msg)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun subscribeToGlobalMessages(callback: MessageCallback) {
        scope.launch {
            try {
                android.util.Log.d("SupabaseService", "Subscribing to global-chat channel")
                val channel = client.channel("global-chat")
                val changeFlow = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
                    table = "global_messages"
                }

                changeFlow.onEach {
                    android.util.Log.d("SupabaseService", "Event received: $it")
                    if (it is PostgresAction.Insert) {
                         try {
                             val msg = it.decodeRecord<MessageDto>()
                             android.util.Log.d("SupabaseService", "New message decoded: $msg")
                             callback.onNewMessage(msg)
                         } catch(e: Exception) {
                             android.util.Log.e("SupabaseService", "Error decoding message: ${e.message}")
                         }
                    }
                }.launchIn(scope)

                channel.subscribe()
                android.util.Log.d("SupabaseService", "Subscribed!")
            } catch (e: Exception) {
                android.util.Log.e("SupabaseService", "Error in subscription: ${e.message}")
                callback.onError(e.message ?: "Subscription error")
            }
        }
        
        // Ensure realtime is connected
        scope.launch {
            try {
                if (client.realtime.status.value != Realtime.Status.CONNECTED) {
                     client.realtime.connect()
                }
            } catch(e: Exception) {
                android.util.Log.e("SupabaseService", "Error connecting to realtime: ${e.message}")
            }
        }
    }

    interface UploadCallback {
        fun onSuccess(publicUrl: String)
        fun onError(message: String)
    }

    fun uploadFile(bucketName: String, fileName: String, data: ByteArray, callback: UploadCallback) {
        scope.launch {
            try {
                // Ensure bucket exists or handled by policy
                val bucket = client.storage.from(bucketName)
                bucket.upload(fileName, data, upsert = true)
                val publicUrl = bucket.publicUrl(fileName)
                callback.onSuccess(publicUrl)
            } catch (e: Exception) {
                callback.onError(e.message ?: "Upload failed")
            }
        }
    }

    fun readBytes(inputStream: java.io.InputStream?): ByteArray {
        if (inputStream == null) return ByteArray(0)
        return inputStream.readBytes()
    }
}

@kotlinx.serialization.Serializable
data class MessageDto(
    val id: String? = null,
    val sender_id: String,
    val sender_name: String,
    val sender_avatar_url: String? = null,
    val message: String?,
    val type: String = "text",
    val file_url: String? = null,
    val duration: Long? = null,
    val created_at: String? = null
)


