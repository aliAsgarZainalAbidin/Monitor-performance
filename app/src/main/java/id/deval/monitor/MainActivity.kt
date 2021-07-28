package id.deval.monitor

import android.app.ActivityManager
import android.content.ContentValues.TAG
import android.content.Context
import android.graphics.Color
import android.net.TrafficStats
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.system.Os.sysconf
import android.system.OsConstants._SC_CLK_TCK
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import androidx.appcompat.app.AppCompatActivity
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.google.android.material.textfield.TextInputLayout
import com.google.android.material.textview.MaterialTextView
import id.deval.monitor.model.AppPackage
import java.io.*

class MainActivity : AppCompatActivity() {

    var listAppsPackage : MutableList<String> = mutableListOf()
    var listApps : MutableList<AppPackage> = mutableListOf()
    lateinit var til_main_app1 : TextInputLayout
    lateinit var til_main_app2 : TextInputLayout
    lateinit var autoc_main_app1 : AutoCompleteTextView
    lateinit var autoc_main_app2 : AutoCompleteTextView
    lateinit var tvMemoryUsageApp1 : MaterialTextView
    lateinit var tvMemoryUsageApp2 : MaterialTextView
    lateinit var activityManager : ActivityManager
    lateinit var lcMemory : LineChart
    lateinit var dsMemory1: LineDataSet
    lateinit var dsMemory2: LineDataSet
    lateinit var dsCpu1: LineDataSet
    lateinit var dsCpu2: LineDataSet
    lateinit var lcCpu : LineChart

    lateinit var mainHandler : Handler
    val memoryUsageData1 = ArrayList<Entry>()
    val memoryUsageData2 = ArrayList<Entry>()
    val cpuUsageData1 = ArrayList<Entry>()
    val cpuUsageData2 = ArrayList<Entry>()
    var counterMemoryData1 = 0f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        listAppsPackage = mutableListOf()
        listApps = mutableListOf()
        mainHandler = Handler(Looper.getMainLooper())

        til_main_app1 = findViewById(R.id.til_main_app1)
        til_main_app2 = findViewById(R.id.til_main_app2)
        autoc_main_app1 = findViewById(R.id.autoc_main_app1)
        autoc_main_app2 = findViewById(R.id.autoc_main_app2)
        tvMemoryUsageApp1 = findViewById(R.id.tv_main_titleMemoryUsage1)
        tvMemoryUsageApp2 = findViewById(R.id.tv_main_titleMemoryUsage2)
        lcMemory = findViewById(R.id.lchart_main_memory)
        lcCpu = findViewById(R.id.lchart_main_cpu)

        dsMemory1 = LineDataSet(arrayListOf(),"Apps1")
        dsMemory2 = LineDataSet(arrayListOf(), "Apps2")
        dsCpu1 = LineDataSet(arrayListOf(),"Apps1")
        dsCpu2 = LineDataSet(arrayListOf(), "Apps2")

        val legend = lcMemory.legend
        legend.isEnabled = true
        legend.setVerticalAlignment(Legend.LegendVerticalAlignment.TOP)
        legend.setHorizontalAlignment(Legend.LegendHorizontalAlignment.CENTER)
        legend.setOrientation(Legend.LegendOrientation.HORIZONTAL)
        legend.setDrawInside(false)

        val legendCpu = lcCpu.legend
        legendCpu.isEnabled = true
        legendCpu.setVerticalAlignment(Legend.LegendVerticalAlignment.TOP)
        legendCpu.setHorizontalAlignment(Legend.LegendHorizontalAlignment.CENTER)
        legendCpu.setOrientation(Legend.LegendOrientation.HORIZONTAL)
        legendCpu.setDrawInside(false)

        lcMemory.description.text = """
            X value in Second  
            Y value in Kb
        """.trimIndent()
        lcMemory.description.textColor = Color.BLUE
        lcMemory.description.textSize = 10f
        lcMemory.description.isEnabled = true
        lcMemory.xAxis.position = XAxis.XAxisPosition.BOTTOM

        lcCpu.description.text = """
            X value in Second  
            Y value in %
        """.trimIndent()
        lcCpu.description.textColor = Color.BLUE
        lcCpu.description.textSize = 10f
        lcCpu.description.isEnabled = true
        lcCpu.xAxis.position = XAxis.XAxisPosition.BOTTOM

        activityManager =
            this.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val pidsTask = activityManager.runningAppProcesses

        for (i in pidsTask.indices) {
            var app = pidsTask[i]
            val packages = AppPackage(app.processName,app.pid)
            listAppsPackage.add(app.processName)
            listApps.add(packages)
        }

        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, listAppsPackage)
        (til_main_app1.editText as? AutoCompleteTextView)?.setAdapter(adapter)
        (til_main_app2.editText as? AutoCompleteTextView)?.setAdapter(adapter)

        autoc_main_app1.setOnItemClickListener { _, _, _, _ ->
            mainHandler.post(updatePerformanceFirstApp)
        }

        autoc_main_app2.setOnItemClickListener { _, _, _, _ ->
            mainHandler.post(updatePerformanceSecondApp)
        }
    }

    private val updatePerformanceFirstApp = object : Runnable{
        override fun run() {
            if (!autoc_main_app1.text.equals("")){
                val value = listApps.find { appPackage ->
                    Log.d(TAG, "onCreate: ${autoc_main_app1.text}")
                    appPackage.packageName.equals(autoc_main_app1.text.toString(), true)
                }?.pid

                val pid: IntArray? = value?.let { nilai -> intArrayOf(nilai) }
                val memoryUsageApp1 = activityManager.getProcessMemoryInfo(pid).get(0).totalPss
                val received = pid?.get(0)?.let { TrafficStats.getUidRxBytes(it) }
                val send = pid?.get(0)?.let { TrafficStats.getUidTxBytes(it) }
                val cpuUsage = readUsage(pid?.get(0).toString())

                tvMemoryUsageApp1.text = """
                Memory Usage  = ${memoryUsageApp1}kB
                Received = $received
                Send = $send
                Cpu Usage = $cpuUsage %
            """.trimIndent()

                memoryUsageData1.add(Entry(counterMemoryData1, memoryUsageApp1.toFloat()))
                dsMemory1 = LineDataSet(memoryUsageData1, autoc_main_app1.text.toString())
                dsMemory1.mode = LineDataSet.Mode.LINEAR
                dsMemory1.color = Color.GREEN
                dsMemory1.setDrawCircles(false)
                dsMemory1.setDrawValues(false)
                lcMemory.data = LineData(dsMemory1,dsMemory2)
                lcMemory.animateXY(0, 0)

                cpuUsageData1.add(Entry(counterMemoryData1, cpuUsage))
                dsCpu1 = LineDataSet(cpuUsageData1, autoc_main_app1.text.toString())
                dsCpu1.mode = LineDataSet.Mode.LINEAR
                dsCpu1.color = Color.GREEN
                dsCpu1.setDrawCircles(false)
                dsCpu1.setDrawValues(false)
                lcCpu.data = LineData(dsCpu1,dsCpu2)
                lcCpu.animateXY(0, 0)

                counterMemoryData1++
            }
            mainHandler.postDelayed(this, 1000)
        }
    }

    private val updatePerformanceSecondApp = object : Runnable{
        override fun run() {
            if (!autoc_main_app2.text.equals("")){
                val valueAutoc2 = listApps.find { appPackage ->
                    appPackage.packageName.equals(autoc_main_app2.text.toString(), true)
                }?.pid

                val pid2 : IntArray? = valueAutoc2?.let { it -> intArrayOf(it) }
                val memoryUsageApp2 = activityManager.getProcessMemoryInfo(pid2).get(0).totalPss
                val receivedApp2 = pid2?.get(0)?.let { TrafficStats.getUidRxBytes(it) }
                val sendApp2 = pid2?.get(0)?.let { TrafficStats.getUidTxBytes(it) }
                val cpuUsageApp2 = readUsage(pid2?.get(0).toString())

                tvMemoryUsageApp2.text = """
                Memory Usage  = ${memoryUsageApp2}kB
                Received = $receivedApp2
                Send = $sendApp2
                Cpu Usage = $cpuUsageApp2 %
            """.trimIndent()

                memoryUsageData2.add(Entry(counterMemoryData1,memoryUsageApp2.toFloat()))
                dsMemory2 = LineDataSet(memoryUsageData2, autoc_main_app2.text.toString())
                dsMemory2.mode = LineDataSet.Mode.LINEAR
                dsMemory2.color = Color.RED
                dsMemory2.setDrawCircles(false)
                dsMemory2.setDrawValues(false)
                lcMemory.data = LineData(dsMemory1,dsMemory2)
                lcMemory.animateXY(0, 0)

                cpuUsageData2.add(Entry(counterMemoryData1, cpuUsageApp2))
                dsCpu2 = LineDataSet(cpuUsageData2, autoc_main_app2.text.toString())
                dsCpu2.mode = LineDataSet.Mode.LINEAR
                dsCpu2.color = Color.RED
                dsCpu2.setDrawCircles(false)
                dsCpu2.setDrawValues(false)
                lcCpu.data = LineData(dsCpu1,dsCpu2)
                lcCpu.animateXY(0, 0)

                counterMemoryData1++
            }
            mainHandler.postDelayed(this,1000)
        }
    }


    private fun readUsage(pid : String): Float {

        try {
            val reader = RandomAccessFile("/proc/uptime", "r")
            var load = reader.readLine()
            var uptime = load.replaceAfter(" ","","").toFloat()

            val readerProcStats = RandomAccessFile("/proc/$pid/stat","r")
            var loadProc = readerProcStats.readLine()
            var indexOf = loadProc.split(" ")
            var utime = indexOf[13]
            var stime = indexOf[14]
            var cutime = indexOf[15]
            val sutime = indexOf[16]
            var startTime = indexOf[21]
            var hertz = sysconf(_SC_CLK_TCK)
            Log.d(TAG, "readUsage: $hertz ${indexOf[21]} ${indexOf[13]} ${indexOf[14]} ${indexOf[15]} ${indexOf[16]}")

            var totalTime = utime.toInt().plus(stime.toInt()).plus(cutime.toInt()).plus(sutime.toInt())
            var seconds = uptime -(startTime.toInt().div(hertz))
            var cpuUsage = 100 * ((totalTime.div(hertz)).div(seconds))
            Log.d(TAG, "readUsage: $cpuUsage %")

            return cpuUsage
        } catch (ex: IOException) {
            ex.printStackTrace()
        }
        return 0f
    }

}