package org.techtown.rtspviewer

import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.SystemClock
import android.util.Log
import android.view.View
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import org.techtown.rtspviewer.databinding.ActivityMainBinding
import org.techtown.rtspviewer.rtsp.RtspEvent
import org.techtown.rtspviewer.rtsp.RtspListener
import org.techtown.rtspviewer.rtsp.RtspViewer
import org.videolan.libvlc.LibVLC
import org.videolan.libvlc.Media
import org.videolan.libvlc.MediaPlayer
import org.videolan.libvlc.interfaces.AbstractVLCEvent
import org.videolan.libvlc.util.VLCVideoLayout

class MainActivity : AppCompatActivity() {
    lateinit var binding: ActivityMainBinding

    val viewerList = ArrayList<RtspViewer>()
    val runningViewerList = arrayListOf<Boolean>()

    val heroViewerList = ArrayList<RtspViewer>()
    val runningHeroViewerList = arrayListOf<Boolean>()

    val urlList = ArrayList<String>()
    val nameList = ArrayList<String>()

    // 2x4 화면 초기화
    val videoLayoutList = ArrayList<VLCVideoLayout>()
    // 2x1 화면 초기화
    val heroVideoLayoutList = ArrayList<VLCVideoLayout>()

    val nameLayoutList = ArrayList<TextView>()
    val heroNameLayoutList = ArrayList<TextView>()

    // 히어로뷰에 보여주기 위해 선택한 URL 인덱스 값
    var selectedUrlIndex = 0

    var pageIndex = 0

    // 프레임 모드 (일반 화면, 히어로 화면)
    enum class FrameMode {
        NORMAL,
        HERO
    }

    // 스크린 모드 (노스트림, 오프닝, HTTP, RTSP)
    enum class ScreenMode {
        NOSTREAM,
        OPENING,
        HTTP,
        RTSP
    }

    val handler = Handler()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 뷰어 객체 초기화
        initRtspViewer()

        // URL 추가
        //var url = "rtsp://wowzaec2demo.streamlock.net/vod/mp4:BigBuckBunny_115k.mp4"
        //urlList.add(url)

        var url = ""

        // CCTV 1 : 서부역 입구 삼거리
        url = "rtsp://210.99.70.120:1935/live/cctv002.stream"
        urlList.add(url)
        nameList.add("서부역 입구 삼거리")

        // CCTV 2 : 역말 오거리
        url = "rtsp://210.99.70.120:1935/live/cctv003.stream"
        urlList.add(url)
        nameList.add("역말 오거리")

        // CCTV 3 : 천안로사거리
        url = "rtsp://210.99.70.120:1935/live/cctv004.stream"
        urlList.add(url)
        nameList.add("천안로사거리")

        // CCTV 4 : 상명대 입구 삼거리
        url = "rtsp://210.99.70.120:1935/live/cctv005.stream"
        urlList.add(url)
        nameList.add("상명대 입구 삼거리")

        // CCTV 5 : 방죽안오거리
        url = "rtsp://210.99.70.120:1935/live/cctv006.stream"
        urlList.add(url)
        nameList.add("방죽안오거리")

        // CCTV 6 : 천안역
        url = "rtsp://210.99.70.120:1935/live/cctv007.stream"
        urlList.add(url)
        nameList.add("천안역")

        // CCTV 7 : 남부오거리
        url = "rtsp://210.99.70.120:1935/live/cctv008.stream"
        urlList.add(url)
        nameList.add("남부오거리")

        // CCTV 8 : 교보사거리
        url = "rtsp://210.99.70.120:1935/live/cctv009.stream"
        urlList.add(url)
        nameList.add("교보사거리")

        // CCTV 9 : 청삼교차로
        url = "rtsp://210.99.70.120:1935/live/cctv010.stream"
        urlList.add(url)
        nameList.add("청삼교차로")

        // CCTV 10 : 신방삼거리
        url = "rtsp://210.99.70.120:1935/live/cctv011.stream"
        urlList.add(url)
        nameList.add("신방삼거리")

        // CCTV 11 : 이마트 사거리
        url = "rtsp://210.99.70.120:1935/live/cctv012.stream"
        urlList.add(url)
        nameList.add("이마트 사거리")

        // CCTV 12 : 쌍용사거리
        url = "rtsp://210.99.70.120:1935/live/cctv013.stream"
        urlList.add(url)
        nameList.add("쌍용사거리")


        // 버튼 초기화
        initButton()



    }

    fun initButton() {

        // 시작 버튼 클릭 시
        binding.startButton.setOnClickListener {

            for (viewerIndex in viewerList.indices) {
                val urlIndex = (pageIndex * viewerList.size) + viewerIndex
                println("urlIndex : ${urlIndex}")

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

        // 이전화면 버튼 클릭 시
        binding.previousPageButton.setOnClickListener {

            if (pageIndex == 0) {
                Toast.makeText(this, "이전 화면이 없습니다.", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            } else {
                pageIndex -= 1
            }

            // 이전 화면 중지
            for (viewerIndex in viewerList.indices) {
                stopVideo(viewerIndex)
            }

            // 1초 후 신규 화면 시작
            handler.postDelayed(Runnable {
                for (viewerIndex in viewerList.indices) {
                    val urlIndex = (pageIndex * viewerList.size) + viewerIndex
                    if (urlIndex < urlList.size) {
                        startVideo(urlIndex, viewerIndex)
                    }
                }
            }, 1000)

        }

        // 다음화면 버튼 클릭 시
        binding.nextPageButton.setOnClickListener {

            if ((pageIndex + 1) * viewerList.size < urlList.size) {
                pageIndex += 1
            } else {
                Toast.makeText(this, "다음 화면이 없습니다.", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            // 이전 화면 중지
            for (viewerIndex in viewerList.indices) {
                stopVideo(viewerIndex)
            }


            // 1초 후 신규 화면 시작
            handler.postDelayed(Runnable {
                for (viewerIndex in viewerList.indices) {
                    val urlIndex = (pageIndex * viewerList.size) + viewerIndex
                    if (urlIndex < urlList.size) {
                        startVideo(urlIndex, viewerIndex)
                    }
                }
            }, 1000)

        }

        // 비디오 레이아웃 1 클릭 시
        binding.videoLayout1.setOnSingleClickListener {
            println("비디오 레이아웃 1 클릭됨.")

            // 현재 index가 0이므로 0, 1을 보여줌
            selectedUrlIndex = (pageIndex * videoLayoutList.size) + 0

            showHeroLayout()
        }

        // 비디오 레이아웃 2 클릭 시
        binding.videoLayout2.setOnSingleClickListener {
            println("비디오 레이아웃 2 클릭됨.")

            // 현재 index가 0이므로 0, 1을 보여줌
            selectedUrlIndex = (pageIndex * videoLayoutList.size) + 0

            showHeroLayout()
        }

        // 비디오 레이아웃 3 클릭 시
        binding.videoLayout3.setOnSingleClickListener {
            println("비디오 레이아웃 3 클릭됨.")

            // 현재 index가 0이므로 0, 1을 보여줌
            selectedUrlIndex = (pageIndex * videoLayoutList.size) + 2

            showHeroLayout()
        }

        // 비디오 레이아웃 4 클릭 시
        binding.videoLayout4.setOnSingleClickListener {
            println("비디오 레이아웃 4 클릭됨.")

            // 현재 index가 0이므로 0, 1을 보여줌
            selectedUrlIndex = (pageIndex * videoLayoutList.size) + 2

            showHeroLayout()
        }

        // 비디오 레이아웃 5 클릭 시
        binding.videoLayout5.setOnSingleClickListener {
            println("비디오 레이아웃 5 클릭됨.")

            // 현재 index가 0이므로 0, 1을 보여줌
            selectedUrlIndex = (pageIndex * videoLayoutList.size) + 4

            showHeroLayout()
        }

        // 비디오 레이아웃 6 클릭 시
        binding.videoLayout6.setOnSingleClickListener {
            println("비디오 레이아웃 6 클릭됨.")

            // 현재 index가 0이므로 0, 1을 보여줌
            selectedUrlIndex = (pageIndex * videoLayoutList.size) + 4

            showHeroLayout()
        }

        // 비디오 레이아웃 7 클릭 시
        binding.videoLayout7.setOnSingleClickListener {
            println("비디오 레이아웃 7 클릭됨.")

            // 현재 index가 0이므로 0, 1을 보여줌
            selectedUrlIndex = (pageIndex * videoLayoutList.size) + 6

            showHeroLayout()
        }

        // 비디오 레이아웃 8 클릭 시
        binding.videoLayout8.setOnSingleClickListener {
            println("비디오 레이아웃 8 클릭됨.")

            // 현재 index가 0이므로 0, 1을 보여줌
            selectedUrlIndex = (pageIndex * videoLayoutList.size) + 6

            showHeroLayout()
        }

        // 큰화면 닫기 버튼 클릭 시
        binding.closeHeroButton.setOnClickListener {

            // 선택된 영상을 0으로
            selectedUrlIndex = 0

            closeHeroLayout()
        }


    }

    /**
     * 히어로 레이아웃 보여주기
     */
    fun showHeroLayout() {

        // 일반 뷰어 중지
        for (viewerIndex in viewerList.indices) {
            stopVideo(viewerIndex)
        }

        // 프레임 모드를 HERO로 설정
        setFrameMode(FrameMode.HERO)

        var heroViewerIndex = 0
        var urlIndex = selectedUrlIndex
        startHeroVideo(urlIndex, heroViewerIndex)

        heroViewerIndex = 1
        urlIndex = selectedUrlIndex + 1
        startHeroVideo(urlIndex, heroViewerIndex)

    }

    /**
     * 히어로 레이아웃 닫기
     */
    fun closeHeroLayout() {

        // 히어로 뷰어 중지
        for (viewerIndex in heroViewerList.indices) {
            stopHeroVideo(viewerIndex)
        }

        // 프레임 모드를 NORMAL로 설정
        setFrameMode(FrameMode.NORMAL)

        for (viewerIndex in viewerList.indices) {
            val urlIndex = (pageIndex * viewerList.size) + viewerIndex
            println("urlIndex : ${urlIndex}")

            if (urlIndex < urlList.size) {
                startVideo(urlIndex, viewerIndex)
            }
        }

    }

    fun showToast(message:String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    /**
     * RTSP 뷰어 객체 초기화하기
     */
    fun initRtspViewer() {
        println("initRtspViewer called.")

        // 프레임 모드를 NORMAL로 설정
        setFrameMode(FrameMode.NORMAL)

        // 2x4 화면 초기화
        videoLayoutList.add(binding.videoLayout1)
        videoLayoutList.add(binding.videoLayout2)
        videoLayoutList.add(binding.videoLayout3)
        videoLayoutList.add(binding.videoLayout4)
        videoLayoutList.add(binding.videoLayout5)
        videoLayoutList.add(binding.videoLayout6)
        videoLayoutList.add(binding.videoLayout7)
        videoLayoutList.add(binding.videoLayout8)

        nameLayoutList.add(binding.name1TextView)
        nameLayoutList.add(binding.name2TextView)
        nameLayoutList.add(binding.name3TextView)
        nameLayoutList.add(binding.name4TextView)
        nameLayoutList.add(binding.name5TextView)
        nameLayoutList.add(binding.name6TextView)
        nameLayoutList.add(binding.name7TextView)
        nameLayoutList.add(binding.name8TextView)

        for (viewer in viewerList) {
            viewer.destroy()
        }
        viewerList.clear()

        for (index in videoLayoutList.indices) {
            createRtspViewer(index, videoLayoutList[index])
        }


        // 2x1 화면 초기화
        heroVideoLayoutList.add(binding.heroVideoLayout1)
        heroVideoLayoutList.add(binding.heroVideoLayout2)

        heroNameLayoutList.add(binding.heroName1TextView)
        heroNameLayoutList.add(binding.heroName2TextView)

        for (viewer in heroViewerList) {
            viewer.destroy()
        }
        heroViewerList.clear()

        createHeroRtspViewer(0, 0, binding.heroVideoLayout1)
        createHeroRtspViewer(1, 1, binding.heroVideoLayout2)

    }

    /**
     * RTSP 뷰어 객체 만들기
     */
    fun createRtspViewer(index: Int, videoLayout: VLCVideoLayout) {
        println("createRtspViewer called : ${index}")

        // RTSP 뷰어 객체 만들기
        val rtspViewer = RtspViewer().also { it ->
            it.init(
                this,
                videoLayout,
                object: RtspListener {
                    override fun onEvent(event: RtspEvent) {
                        when(event) {
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

                                setScreenMode(index, ScreenMode.NOSTREAM)

                                // 만약 시작된 상태라면 재시도
                                if (runningViewerList[index]) {
                                    // 5초 후 재시도
                                    handler.postDelayed(Runnable {
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

    /**
     * RTSP 뷰어 객체 만들기
     */
    fun createHeroRtspViewer(urlIndex: Int, heroIndex: Int, videoLayout: VLCVideoLayout) {
        println("createHeroRtspViewer called : ${urlIndex}, ${heroIndex}")

        // RTSP 뷰어 객체 만들기
        val rtspViewer = RtspViewer().also { it ->
            it.init(
                this,
                videoLayout,
                object: RtspListener {
                    override fun onEvent(event: RtspEvent) {
                        when(event) {
                            RtspEvent.OPENING -> {
                                println("rtsp event #${urlIndex} : opening")

                                setHeroScreenMode(heroIndex, ScreenMode.OPENING)
                            }
                            RtspEvent.PLAYING -> {
                                println("rtsp event #${urlIndex} : playing")
                            }
                            RtspEvent.STARTED -> {
                                println("rtsp event #${urlIndex} : started")

                                setHeroScreenMode(heroIndex, ScreenMode.RTSP)
                            }
                            RtspEvent.PAUSED -> {
                                println("rtsp event #${urlIndex} : paused")
                            }
                            RtspEvent.STOPPED -> {
                                println("rtsp event #${urlIndex} : stopped")

                                setHeroScreenMode(heroIndex, ScreenMode.NOSTREAM)

                                // 만약 시작된 상태라면 재시도
                                if (runningHeroViewerList[heroIndex]) {
                                    // 5초 후 재시도
                                    handler.postDelayed(Runnable {
                                        if (heroIndex == 1) {
                                            selectedUrlIndex += 1
                                        }
                                        val urlIndex = (pageIndex * videoLayoutList.size) + selectedUrlIndex
                                        val viewerIndex = heroIndex
                                        println("retrying to connect : ${urlIndex}, ${viewerIndex}")

                                        startHeroVideo(urlIndex, viewerIndex)
                                    }, 5000)
                                }
                            }
                        }
                    }

                }
            )
        }

        // RTSP 뷰어 객체를 리스트에 추가
        heroViewerList.add(rtspViewer)
        runningHeroViewerList.add(false)
    }

    fun setScreenMode(index:Int, screenMode: ScreenMode) {
        val noStreamLayout = getNoStreamLayout(index)
        val noStreamTextView = getNoStreamTextView(index)
        val videoLayout = videoLayoutList[index]

        when(screenMode) {
            ScreenMode.NOSTREAM -> {
                noStreamLayout.visibility = View.VISIBLE
                videoLayout.visibility = View.INVISIBLE

                noStreamTextView.text = "No Stream"
            }
            ScreenMode.OPENING -> {
                noStreamLayout.visibility = View.VISIBLE
                videoLayout.visibility = View.INVISIBLE

                noStreamTextView.text = "Opening"
            }
            ScreenMode.HTTP -> {

            }
            ScreenMode.RTSP -> {
                noStreamLayout.visibility = View.INVISIBLE
                videoLayout.visibility = View.VISIBLE
            }
        }
    }

    fun getNoStreamLayout(index: Int): RelativeLayout {
        when(index) {
            0 -> {
                return binding.noStreamLayout1
            }
            1 -> {
                return binding.noStreamLayout2
            }
            2 -> {
                return binding.noStreamLayout3
            }
            3 -> {
                return binding.noStreamLayout4
            }
            4 -> {
                return binding.noStreamLayout5
            }
            5 -> {
                return binding.noStreamLayout6
            }
            6 -> {
                return binding.noStreamLayout7
            }
            7 -> {
                return binding.noStreamLayout8
            }
            else -> {
                return binding.noStreamLayout1
            }
        }
    }

    fun getNoStreamTextView(index: Int): TextView {
        when(index) {
            0 -> {
                return binding.noStream1TextView
            }
            1 -> {
                return binding.noStream2TextView
            }
            2 -> {
                return binding.noStream3TextView
            }
            3 -> {
                return binding.noStream4TextView
            }
            4 -> {
                return binding.noStream5TextView
            }
            5 -> {
                return binding.noStream6TextView
            }
            6 -> {
                return binding.noStream7TextView
            }
            7 -> {
                return binding.noStream8TextView
            }
            else -> {
                return binding.noStream1TextView
            }
        }
    }

    fun setHeroScreenMode(index:Int, screenMode: ScreenMode) {
        val noStreamLayout = getHeroNoStreamLayout(index)
        val noStreamTextView = getHeroNoStreamTextView(index)
        val videoLayout = heroVideoLayoutList[index]

        when(screenMode) {
            ScreenMode.NOSTREAM -> {
                noStreamLayout.visibility = View.VISIBLE
                videoLayout.visibility = View.INVISIBLE

                noStreamTextView.text = "No Stream"
            }
            ScreenMode.OPENING -> {
                noStreamLayout.visibility = View.VISIBLE
                videoLayout.visibility = View.INVISIBLE

                noStreamTextView.text = "Opening"
            }
            ScreenMode.HTTP -> {

            }
            ScreenMode.RTSP -> {
                noStreamLayout.visibility = View.INVISIBLE
                videoLayout.visibility = View.VISIBLE
            }
        }
    }

    fun getHeroNoStreamLayout(index: Int): RelativeLayout {
        when(index) {
            0 -> {
                return binding.heroNoStreamLayout1
            }
            1 -> {
                return binding.heroNoStreamLayout2
            }
            else -> {
                return binding.heroNoStreamLayout1
            }
        }
    }

    fun getHeroNoStreamTextView(index: Int): TextView {
        when(index) {
            0 -> {
                return binding.heroNoStream1TextView
            }
            1 -> {
                return binding.heroNoStream2TextView
            }
            else -> {
                return binding.heroNoStream1TextView
            }
        }
    }

    /**
     * 프레임 모드 설정하기
     */
    fun setFrameMode(frameMode: FrameMode) {

        when(frameMode) {
            FrameMode.NORMAL -> {
                binding.frameContainer1.visibility = View.VISIBLE
                binding.frameContainer2.visibility = View.INVISIBLE

                binding.startButton.isEnabled = true
                binding.stopButton.isEnabled = true
                binding.previousPageButton.isEnabled = true
                binding.nextPageButton.isEnabled = true

                binding.closeHeroButton.isEnabled = false
            }
            FrameMode.HERO -> {
                binding.frameContainer1.visibility = View.INVISIBLE
                binding.frameContainer2.visibility = View.VISIBLE

                binding.startButton.isEnabled = false
                binding.stopButton.isEnabled = false
                binding.previousPageButton.isEnabled = false
                binding.nextPageButton.isEnabled = false

                binding.closeHeroButton.isEnabled = true
            }
        }

    }

    /**
     * 비디오 보여주기
     */
    fun startVideo(index:Int, viewerIndex: Int) {
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

    /**
     * 비디오 보여주기
     */
    fun startHeroVideo(index:Int, viewerIndex: Int) {
        println("startHeroVideo called : ${index}, ${viewerIndex}")

        if (index < 0 || index >= urlList.size) {
            println("invalid url index : ${index}")
            return
        }

        if (viewerIndex < 0 || viewerIndex >= heroViewerList.size) {
            println("invalid viewer index : ${viewerIndex}")
            return
        }

        val url = urlList[index]
        val rtspViewer = heroViewerList[viewerIndex]
        rtspViewer.connect(url)

        runningHeroViewerList[viewerIndex] = true

        // 이름 표시
        val nameTextView = heroNameLayoutList[viewerIndex]
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

    fun stopHeroVideo(viewerIndex: Int) {
        println("stopHeroVideo called : ${viewerIndex}")

        if (viewerIndex < 0 || viewerIndex >= heroViewerList.size) {
            println("invalid viewer index : ${viewerIndex}")
            return
        }

        val rtspViewer = heroViewerList[viewerIndex]

        runningHeroViewerList[viewerIndex] = false
        rtspViewer.disconnect()

        // 이름 표시
        val nameTextView = heroNameLayoutList[viewerIndex]
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

    class OnSingleClickListener(private val onClickListener: (view: View) -> Unit) : View.OnClickListener{
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
            if ((onClickedTime-lastClickedTime) < INTERVAL) { return }

            lastClickedTime = onClickedTime
            onClickListener.invoke(view)
        }
    }

    fun View.setOnSingleClickListener(onClickListener: (view: View) -> Unit) {
        setOnClickListener(OnSingleClickListener(onClickListener))
    }

}