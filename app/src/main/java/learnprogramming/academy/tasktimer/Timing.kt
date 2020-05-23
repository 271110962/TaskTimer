package learnprogramming.academy.tasktimer

import android.util.Log
import java.util.*

private const val TAG = "Timing"

class Timing(val taskId: Long, val startTime: Long = Date().time/1000, var id:Long = 0){//working in seconds, not milliseconds

    var duration: Long = 0
    private set

    fun setDuration(){
        duration = Date().time/1000 - startTime
        Log.d(TAG,"$taskId - Start time: $startTime | Duration: $duration")
    }
}