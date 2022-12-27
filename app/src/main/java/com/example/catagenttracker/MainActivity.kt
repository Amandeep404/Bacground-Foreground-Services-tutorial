package com.example.catagenttracker

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.widget.Toast
import android.widget.Toast.LENGTH_SHORT
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import com.example.catagenttracker.RouteTrackingService.Companion.EXTRA_SECRET_CAT_AGENT_ID
import com.example.catagenttracker.worker.CatFurGroomingWorker
import com.example.catagenttracker.worker.CatLitterBoxSittingWorker
import com.example.catagenttracker.worker.CatStretchingWorker
import com.example.catagenttracker.worker.CatSuitUpWorker

class MainActivity : AppCompatActivity() {

    private val workManager = WorkManager.getInstance(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //It tells the WorkManager class to wait for an internet connection before executing work
        val networkConstraints = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()

        val catAgentId = "CatAgent1"

        val catStretchingRequest =
            OneTimeWorkRequest.Builder(CatLitterBoxSittingWorker::class.java)
            .setConstraints(networkConstraints)
                .setInputData(getCatAgentIdInputData(CatStretchingWorker.INPUT_DATA_CAT_AGENT_ID, catAgentId)).build()

        val catFurGroomingRequest =
            OneTimeWorkRequest.Builder(CatFurGroomingWorker::class.java)
                .setConstraints(networkConstraints)
                .setInputData(getCatAgentIdInputData(CatFurGroomingWorker.INPUT_DATA_CAT_AGENT_ID, catAgentId)).build()

        val catLitterBoxSittingRequest =
            OneTimeWorkRequest.Builder(CatLitterBoxSittingWorker::class.java)
            .setConstraints(networkConstraints)
            .setInputData(getCatAgentIdInputData(CatLitterBoxSittingWorker.INPUT_DATA_CAT_AGENT_ID, catAgentId)).build()

        val catSuitRequest =
            OneTimeWorkRequest.Builder(CatSuitUpWorker::class.java)
                .setConstraints(networkConstraints)
                .setInputData(getCatAgentIdInputData(CatSuitUpWorker.INPUT_DATA_CAT_AGENT_ID, catAgentId)).build()

        //Your WorkRequests are now enqueued to be executed in sequence when their
        //constraints are met and the WorkManager class is ready to execute them
        workManager.beginWith(catStretchingRequest)
            .then(catFurGroomingRequest)
            .then(catLitterBoxSittingRequest)
            .then(catSuitRequest)
            .enqueue()

        //To track the progress of the enqueued WorkRequest instances
        workManager.getWorkInfoByIdLiveData(catStretchingRequest.id).observe(this, Observer {
            if (it.state.isFinished){
                showResult("Agent done Stretching")
            }
        })
        workManager.getWorkInfoByIdLiveData(catFurGroomingRequest.id).observe(this, Observer{
            if (it.state.isFinished){
                showResult("Agent done Grooming its fur")
            }
        })
        workManager.getWorkInfoByIdLiveData(catStretchingRequest.id).observe(this, Observer{
            if (it.state.isFinished){
                showResult("Agent done suiting up!!")
                launchTrackingService()
            }
        })



    }

    //This function first observes LiveData for completion updates, showing a result on completion.
    // Then, it defines Intent for launching the service, setting the SCA ID as an extra parameter for that Intent.
    private fun launchTrackingService(){
        RouteTrackingService.trackingCompletion.observe(this, Observer{
            showResult("Agent $it arrived!")
        })
        val serviceIntent = Intent(this, RouteTrackingService::class.java).apply {
            putExtra(EXTRA_SECRET_CAT_AGENT_ID , "007")
        }
        ContextCompat.startForegroundService(this, serviceIntent)
    }

    private fun showResult(message: String) {
        Toast.makeText(this, message, LENGTH_SHORT).show()
    }

    private fun getCatAgentIdInputData(catAgentIdKey : String, catAgentValue : String) =
        Data.Builder().putString(catAgentIdKey, catAgentValue).build()

}