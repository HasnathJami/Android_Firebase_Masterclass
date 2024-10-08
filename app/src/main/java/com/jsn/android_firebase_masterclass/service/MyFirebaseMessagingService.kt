package com.jsn.android_firebase_masterclass.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.jsn.android_firebase_masterclass.LandingActivity
import com.jsn.android_firebase_masterclass.R
import com.jsn.android_firebase_masterclass.utils.BundleKeys
import com.jsn.android_firebase_masterclass.utils.BundleKeys.IS_FROM_FIREBASE_PUSH_KEY
import kotlin.random.Random

class MyFirebaseMessagingService : FirebaseMessagingService() {
    private val TAG = this.javaClass.name

    private var notificationBuilder: NotificationCompat.Builder? = null
    private var notificationManager: NotificationManager? = null
    private var pendingIntent: PendingIntent? = null

    private val CHANNEL_ID = "channel_id"
    private val CHANNEL_NAME = "Firebase_Masterclass"

    companion object {
        private var NOTIFICATION_ID = 6578
        const val FCM_PAYLOAD = "FCM"
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate: ")
        createNotificationChannel()
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("checkForNewToken", token)
        FirebaseMessaging.getInstance().subscribeToTopic("all")
            .addOnCompleteListener { task ->
                var msg = "Subscribed"
                if (!task.isSuccessful) {
                    msg = "Subscription failed"
                }
                Log.d("TAG", msg)
            }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        try {
            NOTIFICATION_ID = Random.nextInt(100000000)
            notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

            try {
                Log.d(TAG, "MESSAGING Data ${remoteMessage.data}")
                Log.d(TAG, "MESSAGING Notification ${remoteMessage.notification}")
                if (remoteMessage.data.isNotEmpty()) {
                    createPendingIntent(remoteMessage.data)
                    fireNotification(remoteMessage.data)
                }
            } catch (e: Exception) {
                Log.d(TAG, "onMessageReceived: ${e.message}")
                e.printStackTrace()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun createPendingIntent(dataPayloadMsg: Map<String, String>? = null) {
        val intent = Intent(this, LandingActivity::class.java)

        intent.putExtra(IS_FROM_FIREBASE_PUSH_KEY, FCM_PAYLOAD)
        dataPayloadMsg?.get("title")?.let {
            intent.putExtra("titleKey", it)
        }
        dataPayloadMsg?.get("messageId")?.let {
            intent.putExtra(BundleKeys.MESSAGE_ID_KEY, it)
        }
        dataPayloadMsg?.get("userId")?.let {
            intent.putExtra(BundleKeys.USER_ID_KEY, it)
        }

        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK

        pendingIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.getActivity(
                this, System.currentTimeMillis().toInt(), intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
        } else {
            PendingIntent.getActivity(
                this, System.currentTimeMillis().toInt(), intent,
                PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
            )
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(NotificationManager::class.java)
            val notificationChannel = NotificationChannel(
                CHANNEL_ID, CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationChannel.setShowBadge(true)
            notificationManager.createNotificationChannel(notificationChannel)
        }
    }

    private fun fireNotification(dataPayloadMsg: Map<String, String>? = null) {
        notificationBuilder = NotificationCompat.Builder(application, CHANNEL_ID)
            .setContentText(dataPayloadMsg?.get("body"))
            .setContentTitle(dataPayloadMsg?.get("title"))
            .setStyle(NotificationCompat.BigTextStyle().bigText(dataPayloadMsg?.get("body")))
            .setContentIntent(pendingIntent)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setColor(ContextCompat.getColor(this, R.color.black))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        if (dataPayloadMsg!!.containsKey("badge")) {
            Log.d(Companion::class.java.name, "fireNotification: $" + dataPayloadMsg["badge"])
            notificationBuilder!!.setBadgeIconType(NotificationCompat.BADGE_ICON_SMALL)
            notificationBuilder!!.setNumber(dataPayloadMsg["badge"]!!.toInt())
        }

        if (dataPayloadMsg.containsKey("image") && dataPayloadMsg["image"]!!.isNullOrEmpty()
                .not()
        ) {
            if (!dataPayloadMsg!!["image"]!!.contains("https")) {
                // if we want to set image from our backend
//            showNotificationWithImage(
//                BaseUrlCollection.Companion.getNOTIFICATION_IMAGE_URL() +
//                        dataPayloadMsg["image"]
//            )

            } else {
                showNotificationWithImage(dataPayloadMsg["image"]!!)
            }
        } else {
            notificationManager?.notify(NOTIFICATION_ID, notificationBuilder?.build())
        }


    }

    private fun showNotificationWithImage(imageUrl: String) {
        Glide.with(applicationContext)
            .asBitmap()
            .load(imageUrl)
            .into(object : CustomTarget<Bitmap?>() {
                override fun onResourceReady(
                    resource: Bitmap,
                    transition: Transition<in Bitmap?>?
                ) {
                    try {
                        notificationBuilder!!.setLargeIcon(resource)
                            .setStyle(
                                NotificationCompat.BigPictureStyle()
                                    .bigPicture(resource)
//                                    .bigLargeIcon(null)

                            )

                        notificationManager!!.notify(NOTIFICATION_ID, notificationBuilder!!.build())
                    } catch (e: java.lang.Exception) {
                        e.printStackTrace()
                        Log.d(
                            MyFirebaseMessagingService.javaClass.name,
                            "onPostExecute: " + e.message
                        )
                    }
                }

                override fun onLoadCleared(placeholder: Drawable?) {
                }
            })


        /* // Coil
        ImageRequest request = new ImageRequest.Builder(getApplicationContext())
                .data(imageUrl)
                .target(new Target() {
                    @Override
                    public void onStart(@org.jetbrains.annotations.Nullable Drawable drawable) {
                    }

                    @Override
                    public void onError(@org.jetbrains.annotations.Nullable Drawable drawable) {
                    }

                    @Override
                    public void onSuccess(@NotNull Drawable drawable) {
                        Bitmap bitmapDrawable = ((BitmapDrawable) drawable).getBitmap();

                        notificationBuilder.setLargeIcon(bitmapDrawable)
                                .setStyle(new NotificationCompat.BigPictureStyle()
                                        .bigPicture(bitmapDrawable)
                                        .bigLargeIcon(null));

                        notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build());
                    }
                })
                .build();
        Coil.imageLoader(getApplicationContext()).enqueue(request);*/
    }


    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy: ")
    }
}

