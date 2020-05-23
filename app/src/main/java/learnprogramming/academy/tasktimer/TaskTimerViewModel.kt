package learnprogramming.academy.tasktimer

import android.app.Application
import android.content.ContentValues
import android.database.ContentObserver
import android.database.Cursor
import android.net.Uri
import android.os.Handler
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

/**
 * Created by timbuchalka for the Android Pie using Kotlin course
 * from www.learnprogramming.academy
 */

private const val TAG = "TaskTimerViewModel"

class TaskTimerViewModel (application: Application) : AndroidViewModel(application) {

    private val contentObserver = object : ContentObserver(Handler()) {
        override fun onChange(selfChange: Boolean, uri: Uri?) {
            Log.d(TAG, "contentObserver.onChange: called. uri is $uri")
            loadTasks()
        }
    }

    private var currentTiming:Timing? = null

    private val databaseCursor = MutableLiveData<Cursor>()
    val cursor: LiveData<Cursor>
        get() = databaseCursor

    private val taskTiming = MutableLiveData<String>()
    val timing: LiveData<String>
        get() = taskTiming


    init {
        Log.d(TAG, "TaskTimerViewModel: created")
        getApplication<Application>().contentResolver.registerContentObserver(TasksContract.CONTENT_URI,
                 true, contentObserver)

        currentTiming = retrieveTiming()
        loadTasks()

    }


    private fun loadTasks() {
        val projection = arrayOf(TasksContract.Columns.ID,
                TasksContract.Columns.TASK_NAME,
                TasksContract.Columns.TASK_DESCRIPTION,
                TasksContract.Columns.TASK_SORT_ORDER)
        // <order by> Tasks.SortOrder, Tasks.Name
        val sortOrder = "${TasksContract.Columns.TASK_SORT_ORDER}, ${TasksContract.Columns.TASK_NAME}"

        GlobalScope.launch {
        val cursor = getApplication<Application>().contentResolver.query(
                TasksContract.CONTENT_URI,
                projection, null, null,
                sortOrder)
        databaseCursor.postValue(cursor)
        }
    }

    fun saveTask(task: Task): Task {
        val values = ContentValues()

        if (task.name.isNotEmpty()) {
            // Don't save a task wth no name
            values.put(TasksContract.Columns.TASK_NAME, task.name)
            values.put(TasksContract.Columns.TASK_DESCRIPTION, task.description)
            values.put(TasksContract.Columns.TASK_SORT_ORDER, task.sortOrder)  // defaults to zero if empty

            if (task.id == 0L) {
                GlobalScope.launch {
                    Log.d(TAG, "saveTask: adding new task")
                    val uri = getApplication<Application>().contentResolver?.insert(TasksContract.CONTENT_URI, values)
                    if (uri != null) {
                        task.id = TasksContract.getId(uri)
                        Log.d(TAG, "saveTask: new id is ${task.id}")
                    }
                }
            } else {
                // task has an id, so we're updating
                GlobalScope.launch {
                    Log.d(TAG, "saveTask: updating task")
                    getApplication<Application>().contentResolver?.update(TasksContract.buildUriFromId(task.id), values, null, null)
                }
            }
        }
        return task
    }

    fun deleteTask(taskId: Long) {
        Log.d(TAG, "Deleting task")
        GlobalScope.launch {
            getApplication<Application>().contentResolver?.delete(TasksContract.buildUriFromId(taskId), null, null)
        }
    }

    fun timeTask(task: Task){
        Log.d(TAG,"timeTask: called")
        val timingRecord = currentTiming

        if(timingRecord == null){
            //没有task，开始一个新的task进行timing
            currentTiming = Timing(task.id)
            saveTiming(currentTiming!!)
            Log.d(TAG,"000000000000000")
        }else{
            //保存这个task
            timingRecord.setDuration()
            saveTiming(timingRecord)
            Log.d(TAG,"11111111111111111")

            if(task.id == timingRecord.taskId){
                //长按按钮，停止timing
                currentTiming = null
                Log.d(TAG,"22222222222222")
            }else{
                //如果id不相等，则意味着一个新的task被timed
                val newTiming = Timing(task.id)
                 saveTiming(newTiming)
                currentTiming = newTiming
                Log.d(TAG,"33333333333333")
            }
        }

        //Update the LiveData:timing
        taskTiming.value = if(currentTiming != null) task.name else null
    }


    private fun saveTiming(currentTiming: Timing){
        Log.d(TAG,"saveTiming: called")

        val inserting = (currentTiming.duration == 0L)

        val values = ContentValues().apply{
            if(inserting) {
                put(TimingsContract.Columns.TIMING_TASK_ID, currentTiming.taskId)
                put(TimingsContract.Columns.TIMING_START_TIME, currentTiming.startTime)
            }
            put(TimingsContract.Columns.TIMING_DURATION,currentTiming.duration)
        }

        GlobalScope.launch{
            if(inserting){
                Log.d(TAG,"进入insert")
                val uri = getApplication<Application>().contentResolver.insert(TimingsContract.CONTENT_URI,values)
                if(uri != null){
                    currentTiming.id = TimingsContract.getId(uri)
                }
            }
            else{
                Log.d(TAG,"进入update")
                getApplication<Application>().contentResolver.update(TimingsContract.buildUriFromId(currentTiming.id),values,null,null)
            }
        }
    }


    private fun retrieveTiming(): Timing?{
        Log.d(TAG,"retrieveTiming starts")

        val timing: Timing?

        val timingCursor: Cursor? = getApplication<Application>().contentResolver.query(CurrentTimingContract.CONTENT_URI,
                null,null,null,null)

        if(timingCursor != null && timingCursor.moveToFirst()){
            //有一个没有被timed的record:意味着duration为0， 因为CurrentTimingContract记录的都是duration为0的task
            val id = timingCursor.getLong(timingCursor.getColumnIndex(CurrentTimingContract.Columns.TIMING_ID))
            val taskId = timingCursor.getLong(timingCursor.getColumnIndex(CurrentTimingContract.Columns.TASK_ID))
            val startTime = timingCursor.getLong(timingCursor.getColumnIndex(CurrentTimingContract.Columns.START_TIME))
            val taskName = timingCursor.getString(timingCursor.getColumnIndex(CurrentTimingContract.Columns.TASK_NAME))
            //因为某种原因暂停app之后会造成timing中断，通过cursor拿到数据，重新生成一个timing
            timing = Timing(taskId,startTime,id)

            //Update the LiveData
            taskTiming.value = taskName
        }else{
            //没有找到duration为0的timing record
            timing =null
        }
        timingCursor?.close()

        Log.d(TAG,"retrieveTiming returning")
        return timing
    }



    override fun onCleared() {
        Log.d(TAG, "onCleared: called")
        getApplication<Application>().contentResolver.unregisterContentObserver(contentObserver)
    }
}