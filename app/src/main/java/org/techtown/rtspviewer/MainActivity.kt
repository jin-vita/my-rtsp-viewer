package org.techtown.rtspviewer

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.View
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import org.techtown.rtspviewer.databinding.ActivityMainBinding
import org.techtown.rtspviewer.rtsp.RtspEvent
import org.techtown.rtspviewer.rtsp.RtspListener
import org.techtown.rtspviewer.rtsp.RtspViewer
import org.videolan.libvlc.util.VLCVideoLayout
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.*
import kotlin.concurrent.timer

class MainActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "MainActivity"
    }

    lateinit var binding: ActivityMainBinding

    val viewerList = ArrayList<RtspViewer>()
    val runningViewerList = arrayListOf<Boolean>()
    val urlList = ArrayList<String>()
    val nameList = ArrayList<String>()

    // 2x4 화면 초기화
    val videoLayoutList = ArrayList<VLCVideoLayout>()

    val nameLayoutList = ArrayList<TextView>()

    var pageIndex = 0

    var camIpMap = mutableMapOf<String, Pair<String, String>>()

    val camId = listOf("LTR-SMC-001-F", "LTR-SMC-001-B")

    private lateinit var timer: Timer

    // 스크린 모드 (노스트림, 오프닝, HTTP, RTSP)
    enum class ScreenMode {
        NO_STREAM,
        OPENING,
        HTTP,
        RTSP
    }

    val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 뷰어 객체 초기화
        initRtspViewer()

        // 버튼 초기화
        initButton()

        camId.forEach { getCamIp(it) }
        timer = timer(period = 3000) {
            if (camIpMap.size == 2) {
                timer.cancel()
                setAndStart()
            }
        }
    }

    private fun setAndStart() {
        // CCTV 0 : 앞
        camId.onEach {
            camIpMap.get(it)?.let { (name, ip) ->
                urlList.add("rtsp://$ip:1935")
                nameList.add(name)
            }
        }
        AppData.error(TAG, "ipMap : $camIpMap")
        runOnUiThread { binding.startButton.performClick() }
    }

    private fun initButton() {

        // 시작 버튼 클릭 시
        binding.startButton.setOnClickListener {

            for (viewerIndex in viewerList.indices) {
                val urlIndex = (pageIndex * viewerList.size) + viewerIndex
                println("urlIndex : $urlIndex")

                if (urlIndex < urlList.size) {
                    startVideo(urlIndex, viewerIndex)
                }
            }

        }

        // 중지 버튼 클릭 시
        binding.stopButton.setOnClickListener {

            for (viewerIndex in viewerList.indices) {
                stopVideo(viewerIndex)
            }

        }

        // 비디오 레이아웃 1 클릭 시
        binding.videoLayout1.setOnSingleClickListener {
            showToast("비디오 레이아웃 1 클릭됨.")
        }

        // 비디오 레이아웃 2 클릭 시
        binding.videoLayout2.setOnSingleClickListener {
            showToast("비디오 레이아웃 2 클릭됨.")
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    /**
     * RTSP 뷰어 객체 초기화하기
     */
    private fun initRtspViewer() {
        println("initRtspViewer called.")

        binding.startButton.isEnabled = true
        binding.stopButton.isEnabled = true

        // 2x4 화면 초기화
        videoLayoutList.add(binding.videoLayout1)
        videoLayoutList.add(binding.videoLayout2)

        nameLayoutList.add(binding.name1TextView)
        nameLayoutList.add(binding.name2TextView)

        for (viewer in viewerList) {
            viewer.destroy()
        }
        viewerList.clear()

        for (index in videoLayoutList.indices) {
            createRtspViewer(index, videoLayoutList[index])
        }
    }

    private fun getCamIp(id: String) {
        val method = object {}.javaClass.enclosingMethod?.name
        RobotClient.api.getCamIp("123", id).enqueue(object : Callback<DeviceInfoData> {
            override fun onResponse(call: Call<DeviceInfoData>, response: Response<DeviceInfoData>) {
                val code = response.code()
                if (response.isSuccessful) response.body()?.data?.apply {
                    if (isNotEmpty()) get(0).apply { camIpMap[id] = name to ip }
                } else AppData.error(TAG, "$method isNotSuccessful. code: $code")
            }

            override fun onFailure(call: Call<DeviceInfoData>, t: Throwable) {
                AppData.error(TAG, "$method isFail, ${t.message}")
            }
        })
    }

    /**
     * RTSP 뷰어 객체 만들기
     */
    private fun createRtspViewer(index: Int, videoLayout: VLCVideoLayout) {
        println("createRtspViewer called : ${index}")

        // RTSP 뷰어 객체 만들기
        val rtspViewer = RtspViewer().also {
            it.init(
                this,
                videoLayout,
                object : RtspListener {
                    override fun onEvent(event: RtspEvent) {
                        when (event) {
                            RtspEvent.OPENING -> {
                                println("rtsp event #${index} : opening")

                                setScreenMode(index, ScreenMode.OPENING)
                            }

                            RtspEvent.PLAYING -> {
                                println("rtsp event #${index} : playing")
                            }

                            RtspEvent.STARTED -> {
                                println("rtsp event #${index} : started")

                                setScreenMode(index, ScreenMode.RTSP)
                            }

                            RtspEvent.PAUSED -> {
                                println("rtsp event #${index} : paused")
                            }

                            RtspEvent.STOPPED -> {
                                println("rtsp event #${index} : stopped")

                                setScreenMode(index, ScreenMode.NO_STREAM)

                                // 만약 시작된 상태라면 재시도
                                if (runningViewerList[index]) {
                                    // 5초 후 재시도
                                    handler.postDelayed({
                                        val urlIndex = (pageIndex * viewerList.size) + index
                                        val viewerIndex = index
                                        println("retrying to connect : ${urlIndex}, ${viewerIndex}")

                                        startVideo(urlIndex, viewerIndex)
                                    }, 5000)
                                }
                            }
                        }
                    }
                }
            )
        }
        // RTSP 뷰어 객체를 리스트에 추가
        viewerList.add(rtspViewer)
        runningViewerList.add(false)
    }

    fun setScreenMode(index: Int, screenMode: ScreenMode) {
        val noStreamLayout = getNoStreamLayout(index)
        val noStreamTextView = getNoStreamTextView(index)
        val videoLayout = videoLayoutList[index]

        when (screenMode) {
            ScreenMode.NO_STREAM -> {
                noStreamLayout.visibility = View.VISIBLE
                videoLayout.visibility = View.INVISIBLE
                noStreamTextView.text = "No Stream"
            }

            ScreenMode.OPENING -> {
                noStreamLayout.visibility = View.VISIBLE
                videoLayout.visibility = View.INVISIBLE
                noStreamTextView.text = "Opening"
            }

            ScreenMode.HTTP -> {}

            ScreenMode.RTSP -> {
                noStreamLayout.visibility = View.INVISIBLE
                videoLayout.visibility = View.VISIBLE
            }
        }
    }

    private fun getNoStreamLayout(index: Int): RelativeLayout = when (index) {
        0 -> binding.noStreamLayout1
        else -> binding.noStreamLayout2
    }

    private fun getNoStreamTextView(index: Int): TextView = when (index) {
        0 -> binding.noStream1TextView
        else -> binding.noStream2TextView
    }

    /**
     * 비디오 보여주기
     */
    fun startVideo(index: Int, viewerIndex: Int) {
        println("startVideo called : ${index}, ${viewerIndex}")

        if (index < 0 || index >= urlList.size) {
            println("invalid url index : ${index}")
            return
        }

        if (viewerIndex < 0 || viewerIndex >= viewerList.size) {
            println("invalid viewer index : ${viewerIndex}")
            return
        }

        val url = urlList[index]
        val rtspViewer = viewerList[viewerIndex]
        rtspViewer.connect(url)

        runningViewerList[viewerIndex] = true

        // 이름 표시
        val nameTextView = nameLayoutList[viewerIndex]
        nameTextView.text = nameList[index]
    }

    fun stopVideo(viewerIndex: Int) {
        println("stopVideo called : ${viewerIndex}")

        if (viewerIndex < 0 || viewerIndex >= viewerList.size) {
            println("invalid viewer index : ${viewerIndex}")
            return
        }

        val rtspViewer = viewerList[viewerIndex]

        runningViewerList[viewerIndex] = false
        rtspViewer.disconnect()

        // 이름 표시
        val nameTextView = nameLayoutList[viewerIndex]
        nameTextView.text = ""
    }

    override fun onStop() {
        super.onStop()

        for (index in viewerList.indices) {
            viewerList[index].disconnect()
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        for (index in viewerList.indices) {
            viewerList[index].destroy()
        }
    }

    class OnSingleClickListener(private val onClickListener: (view: View) -> Unit) : View.OnClickListener {
        companion object {
            // 버튼 사이에 허용하는 시간간격
            const val INTERVAL = 200L
        }

        // 이전 클릭 시간 기록
        private var lastClickedTime = 0L

        override fun onClick(view: View) {
            // 클릭 시간
            val onClickedTime = SystemClock.elapsedRealtime()
            // 간격보다 작으면 클릭 no
            if ((onClickedTime - lastClickedTime) < INTERVAL) {
                return
            }
            lastClickedTime = onClickedTime
            onClickListener.invoke(view)
        }
    }

    private fun View.setOnSingleClickListener(onClickListener: (view: View) -> Unit) {
        setOnClickListener(OnSingleClickListener(onClickListener))
    }
}