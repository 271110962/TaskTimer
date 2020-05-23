package learnprogramming.academy.tasktimer

import android.os.Bundle
import android.preference.PreferenceManager.getDefaultSharedPreferences
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatDialogFragment
import kotlinx.android.synthetic.main.settings_dialog.*
import java.util.*

private const val TAG = "SettingsDialog"

const val SETTINGS_FIRST_DAY_OF_WEEK ="FirstDay"
const val SETTINGS_IGNORE_LESS_THAN = "IgnoreLessThan"
const val SETTINGS_DEFAULT_IGNORE_LESS_THAN = 0

private val deltas = intArrayOf(0,5,10,15,20,25,30,35,40,45,50,55,60,120,180,240,300,360,420,480,540,600,900,1800,2700)//对应seekbar的24个数值，现在用秒代替。
class SettingsDialog: AppCompatDialogFragment() {

    private val defaultFirstDayOfWeek = GregorianCalendar(Locale.getDefault()).firstDayOfWeek
    private var firstDay = defaultFirstDayOfWeek
    private var ignoreLessThan = SETTINGS_DEFAULT_IGNORE_LESS_THAN

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG,"onCreate: called")
        super.onCreate(savedInstanceState)
        setStyle(AppCompatDialogFragment.STYLE_NORMAL,R.style.SettingsDialogStyle)
        retainInstance = true//添加了这个之后，appdialog不会再destroy，而是再rotating之后直接call onCreateView（）
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        Log.d(TAG,"onCreateView: called")
        return inflater.inflate(R.layout.settings_dialog, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        Log.d(TAG,"onViewCreate: called")
        super.onViewCreated(view, savedInstanceState)

        dialog?.setTitle(R.string.action_settings)

        okButton.setOnClickListener{
            saveValues()
            dismiss()
        }

        cancelButton.setOnClickListener{
            dismiss()
        }

        ignoreSeconds.setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener{
            override fun onStartTrackingTouch(p0: SeekBar?) {
                //不需要
            }

            override fun onStopTrackingTouch(p0: SeekBar?) {
                //不需要
            }

            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if(progress < 12){
                    ignoreSecondsTitle.text = getString(R.string.settingsIgnoreSecondsTitle,
                            deltas[progress],
                            resources.getQuantityString(R.plurals.settingsLittleUnits,deltas[progress]))
                }else{
                    val minutes = deltas[progress]/60
                    ignoreSecondsTitle.text = getString(R.string.settingsIgnoreSecondsTitle,
                            minutes,
                            resources.getQuantityString(R.plurals.settingsBigUnits,minutes))
                }
            }

        })
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        Log.d(TAG,"onViewStateRestored: called")
        super.onViewStateRestored(savedInstanceState)

        if(savedInstanceState == null) {
            readValues()

            firstDaySpinner.setSelection(firstDay - GregorianCalendar.SUNDAY)

            //读取seekbar的时候由于之前是是读取的秒，所以在这里需要将秒转换为position value才能应用在seekbar上显示给用户
            val seekBarValue = deltas.binarySearch(ignoreLessThan)
            if (seekBarValue < 0) {
                throw IndexOutOfBoundsException("Value $seekBarValue not found in deltas array")
            }
            ignoreSeconds.max = deltas.size - 1

            Log.d(TAG, "onViewStateRestored(): setting silder to $seekBarValue")
            ignoreSeconds.progress = seekBarValue

            if (ignoreLessThan < 60) {
                ignoreSecondsTitle.text = getString(R.string.settingsIgnoreSecondsTitle, ignoreLessThan, resources.getQuantityString(R.plurals.settingsLittleUnits, ignoreLessThan))
            } else {
                val minutes = ignoreLessThan / 60
                ignoreSecondsTitle.text = getString(R.string.settingsIgnoreSecondsTitle, minutes, resources.getQuantityString(R.plurals.settingsBigUnits, minutes))
            }
        }
    }

    private fun readValues(){
        with(getDefaultSharedPreferences(context)){
            firstDay = getInt(SETTINGS_FIRST_DAY_OF_WEEK,defaultFirstDayOfWeek)//右边的参数为默认参数，用在一开始的时候
            ignoreLessThan = getInt(SETTINGS_IGNORE_LESS_THAN, SETTINGS_DEFAULT_IGNORE_LESS_THAN)
        }
        Log.d(TAG,"readValues(): Saving first day = $firstDay, ignore seconds = $ignoreLessThan")
    }

    private fun saveValues(){
        val newFirstDay = firstDaySpinner.selectedItemPosition + GregorianCalendar.SUNDAY
        val newIgnoreLessThan = deltas[ignoreSeconds.progress]//用秒代替之前的position value

        Log.d(TAG,"SaveValues(): Saving first day = $newFirstDay, ignore seconds = $newIgnoreLessThan")

        with(getDefaultSharedPreferences(context).edit()){
            if(newFirstDay != firstDay){
                putInt(SETTINGS_FIRST_DAY_OF_WEEK,newFirstDay)
            }
            if(newIgnoreLessThan != ignoreLessThan){
                putInt(SETTINGS_IGNORE_LESS_THAN,newIgnoreLessThan)
            }
            apply()
        }
    }

    override fun onDestroy() {
        Log.d(TAG,"onDestroy:called")
        super.onDestroy()
    }
}