package com.example.catagenttracker

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.*
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.Builder
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData


class RouteTrackingService : Service() {

    private lateinit var notificationBuilder: NotificationCompat.Builder
    private lateinit var serviceHandler: Handler

    //You will not rely on binding in this exercise, so it is safe to simply return null in
    //the onBind(Intent) implementation
    override fun onBind(p0: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        // FOREGROUND SERVICE
        notificationBuilder = startForegroundService()

        //to create the Handler instance, you must first define and start HandlerThread.
        val handlerThread = HandlerThread("RouteTracking").apply {
            start()
        }
        serviceHandler = Handler(handlerThread.looper)
    }

    //This will launch MainActivity whenever the user clicks on Notification.
    private fun getPendingIntent() =
        PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), 0)

    @RequiresApi (Build.VERSION_CODES.O)
    private fun createNotificationChannel() : String{
        val channelId = "routeTracking" // channel-ID needs to be unique to a package
        val channelName = "Route tracking"
        val channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_DEFAULT)
        val service = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        service.createNotificationChannel(channel)
        return channelId //The function returns the channel ID so that it can be used to construct Notification.
    }

    private fun getNotificationBuilder(pendingIntent: PendingIntent,channelId: String ) =
        NotificationCompat.Builder(this, channelId)
            .setContentTitle("Agent approaching Destination")
            .setContentText("Agent Dispatched")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setTicker("Agent Dispatched, tracking movement")

    //function to start the foreground service
    private fun startForegroundService() : NotificationCompat.Builder{
        val pendingIntent = getPendingIntent()
        val channelId =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
                createNotificationChannel()
            }else {
                " "
            }
        val notificationBuilder = getNotificationBuilder(pendingIntent, channelId)
        startForeground(NOTIFICATION_ID, notificationBuilder.build())
        return notificationBuilder
    }

    private fun trackToDestination(notificationBuilder: NotificationCompat.Builder){
        for(i in 10 downTo 0){ //This will count down from 10 to 1, sleeping for 1 second between updates and then updating the notification with the remaining time.
            Thread.sleep(1000L)
            notificationBuilder
                .setContentText("$i seconds to destination")
            startForeground(NOTIFICATION_ID, notificationBuilder.build())

        }
    }


    private fun notifyCompletion(agentId: String){
        Handler(Looper.getMainLooper()).post{
            mutableTrackingCompletion.value = agentId
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val returnValue = super.onStartCommand(intent, flags, startId)
        val agentId = intent?.getStringExtra(EXTRA_SECRET_CAT_AGENT_ID)
            ?: throw IllegalStateException("Agent Id must be provided")
        serviceHandler.post{
            trackToDestination(notificationBuilder)
            notifyCompletion(agentId)
            stopForeground(true)
            stopSelf()
        }
        return returnValue
    }


    companion object{
        //NOTIFICATION_ID has to be a unique identifier for the notification owned by this service and must not be 0.
        const val NOTIFICATION_ID = 0xCA7
        //EXTRA_SECRET_CAT_AGENT_ID is the constant you would use to pass data to the service
        const val EXTRA_SECRET_CAT_AGENT_ID = "scaId"

        //LiveData instance used to observe progress
        private val mutableTrackingCompletion = MutableLiveData<String>()
        val trackingCompletion : LiveData<String> = mutableTrackingCompletion
    }
}