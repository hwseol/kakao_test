package com.example.mykacao

import android.Manifest
import android.app.AlertDialog
import android.content.*
import android.content.pm.*
import android.graphics.Color
import android.location.*
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.*
import android.view.ContextMenu.ContextMenuInfo
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.mykacao.databinding.ActivityMainBinding
import net.daum.mf.map.api.*
import okhttp3.OkHttpClient
import retrofit2.*
import retrofit2.converter.gson.GsonConverterFactory
import kotlin.concurrent.thread


val GPS_ENABLE_REQUEST_CODE = 2001
val PERMISSIONS_REQUEST_CODE = 100
var REQUIRED_PERMISSIONS = arrayOf<String>( Manifest.permission.READ_EXTERNAL_STORAGE)
var dLatitute = 0.0
var dLongitute = 0.0
var dGpsLatitute = 0.0
var dGpsLongitute = 0.0
var beforeLoad = true

class MainActivity : AppCompatActivity() , MapView.CurrentLocationEventListener, MapView.MapViewEventListener , MapView.POIItemEventListener{
    private lateinit var binding : ActivityMainBinding
    private val listItems = arrayListOf<ListLayout>()
    private var mapView: MapView? = null
    private val MIN_DISTANCE_CHANGE_FOR_UPDATES: Long = 1  //거리
    private val MIN_TIME_BW_UPDATES = (3000 * 1 * 1).toLong()  //시간

    companion object {
        const val BASE_URL = "https://dapi.kakao.com/"
        const val API_KEY = "KakaoAK " + "910bfdcec83123df6255943bab4168c9"  // REST API 키
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        mapView = MapView(this)
        val mapViewContainer = findViewById<View>(R.id.map_view) as ViewGroup
        mapViewContainer.addView(mapView)

        var main_btn : Button = findViewById<Button>(R.id.button1)
        var hospital_btn : Button = findViewById<Button>(R.id.bHospital)
        var pharmacy_btn : Button = findViewById<Button>(R.id.bPharmacy)
        var gasstation_btn : Button = findViewById<Button>(R.id.bGasstation)
        var refresh_btn : Button = findViewById<Button>(R.id.bRefresh)

        main_btn.setOnClickListener {
            showMarkDetails(1)
            goToPoint(dLatitute, dLongitute)
        }

        hospital_btn.setOnClickListener {
            hospital_btn.isSelected
            pharmacy_btn.isActivated
            gasstation_btn.isActivated
            beforeLoad = false
            searchKeyword("HP8")
            goToPoint(dLatitute, dLongitute)
        }

        pharmacy_btn.setOnClickListener {
            hospital_btn.isActivated
            pharmacy_btn.isSelected
            gasstation_btn.isActivated
            beforeLoad = false
            searchKeyword("PM9")
            goToPoint(dLatitute, dLongitute)
        }

        gasstation_btn.setOnClickListener {
            hospital_btn.isActivated
            pharmacy_btn.isActivated
            gasstation_btn.isSelected
            beforeLoad = false
            searchKeyword("OL7")
            goToPoint(dLatitute, dLongitute)
        }

        refresh_btn.setOnClickListener {
            mapRefresh()
        }

        mapView!!.setMapViewEventListener(this)
        mapView!!.setCurrentLocationEventListener(this)
        mapView!!.setPOIItemEventListener(this)
        mapView!!.setCurrentLocationTrackingMode(MapView.CurrentLocationTrackingMode.TrackingModeOnWithoutHeadingWithoutMapMoving)

        loading()//get Gps point
    }

    private fun loading() {
        Toast.makeText(this, "맵을 로딩중입니다", Toast.LENGTH_SHORT).show();
        var subList: MutableList<ListLayout> = ArrayList()
        subList.add(ListLayout("병원 / 약국/ 주유소 중","알고싶은 시설 정보 선택","",0.0,0.0))
        val adapter = ListViewAdpater(this, subList)
        binding.listViewTest.adapter = adapter

        if (!checkLocationServicesStatus()) {
            showDialogForLocationServiceSetting()
        }else {
            checkRunTimePermission()
        }

        thread(start = true) {
            while(dGpsLatitute == 0.0 && dGpsLongitute == 0.0){
                Log.d("Test", "Thread! dGpsLatitute" + dGpsLatitute + "dGpsLongitute" + dGpsLongitute)
                Thread.sleep(1000)
            }
            dLatitute = dGpsLatitute
            dLongitute = dGpsLongitute
            goToPoint(dLatitute, dLongitute) //goto Gps point
        }
    }

//    private fun getGps(){
//        val locationManager : LocationManager = getSystemService(LOCATION_SERVICE) as LocationManager
//        var gpsLocation : Location? = null
//        val gpsListener: LocationListener = object : LocationListener {
//            override fun onLocationChanged(location: Location) {
//                Log.d("Test", "GPS Location changed, onLocationChanged()")
//                dGpsLatitute = location.latitude
//                dGpsLongitute = location.longitude
//                Log.d("Test", "GPS Location changed, Latitude: $dGpsLatitute" + ", Longitude: $dGpsLongitute")
//                //showGpsPoint()
//            }
//
//            override fun onStatusChanged(provider: String, status: Int, extras: Bundle) {
//                Log.d("Test", "GPS Location changed, onStatusChanged()")
//            }
//            override fun onProviderEnabled(provider: String) {
//                Log.d("Test", "GPS Location changed, onProviderEnabled()")
//            }
//            override fun onProviderDisabled(provider: String) {
//                Log.d("Test", "GPS Location changed, onProviderDisabled()")
//            }
//        }
//
//        if (!checkLocationServicesStatus()) {
//            showDialogForLocationServiceSetting()
//        }else {
//            checkRunTimePermission()
//        }
//        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, MIN_TIME_BW_UPDATES.toLong(), MIN_DISTANCE_CHANGE_FOR_UPDATES.toFloat(), gpsListener)
//        gpsLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
//        if (gpsLocation != null) {
//            dGpsLatitute = gpsLocation.latitude
//            dGpsLongitute = gpsLocation.longitude
//            dLatitute = gpsLocation.latitude
//            dLongitute = gpsLocation.longitude
//        }
//        Log.d("Test", "GPS Loaction: ${dGpsLatitute} / ${dGpsLongitute}")
//        //showGpsPoint()
//    }

    private fun goToPoint(vLatitute: Double, vLongitute: Double) {
        Log.d(
            "Test", "goToPoint [ " + vLatitute + " ] / [ " + vLongitute + "]"
        )
        //mapView!!.setMapCenterPointAndZoomLevel(MapPoint.mapPointWithGeoCoord(vLatitute, vLongitute), 4,true);
        mapView!!.setMapCenterPoint(MapPoint.mapPointWithGeoCoord(vLatitute, vLongitute), false);
        val uNowPosition = MapPoint.mapPointWithGeoCoord(vLatitute, vLongitute)
        // app poi to map (center point)
        val marker = MapPOIItem()
        marker.itemName = "중심점"
        marker.mapPoint =uNowPosition
        marker.markerType = MapPOIItem.MarkerType.BluePin
        marker.selectedMarkerType = MapPOIItem.MarkerType.RedPin
        mapView!!.addPOIItem(marker)
    }

    private fun showGpsPoint() {
        Log.d(
            "Test", "showGpsPoint [ " + dGpsLatitute + " ] / [ " + dGpsLongitute + "]"
        )
        val uNowPosition = MapPoint.mapPointWithGeoCoord(dGpsLatitute, dGpsLongitute)
        // app poi to map (position now gps)
        val marker = MapPOIItem()
        marker.itemName = "현 위치"
        marker.mapPoint =uNowPosition
        marker.markerType = MapPOIItem.MarkerType.RedPin
        marker.selectedMarkerType = MapPOIItem.MarkerType.RedPin
        mapView!!.addPOIItem(marker)
    }


    private fun goToClickPoint(vLatitute: Double, vLongitute: Double) {
        Log.d(
            "Test", "goToClickPoint [ " + vLatitute + " ] / [ " + vLongitute + "]"
        )
        //mapView!!.setMapCenterPointAndZoomLevel(MapPoint.mapPointWithGeoCoord(vLatitute, vLongitute), 4,true);
        mapView!!.setMapCenterPoint(MapPoint.mapPointWithGeoCoord(vLatitute, vLongitute), false);
        val uNowPosition = MapPoint.mapPointWithGeoCoord(vLatitute, vLongitute)
    }

    private fun mapRefresh() {
        var emptyList: MutableList<ListLayout> = ArrayList()
        val adapter = ListViewAdpater(this, emptyList)
        var refresh_btn: Button = findViewById<Button>(R.id.bRefresh)

        mapView?.removeAllPOIItems()
        binding.listViewTest.adapter = adapter // show init text

        loading()
        //getGps() //get GPS point
        //goToPoint(dGpsLatitute, dGpsLongitute) //goto Gps point

        refresh_btn.setVisibility(View.GONE) //refresh button gone
        beforeLoad=true //flag return to before click
    }

    private fun searchKeyword(keyword: String) {
        val retrofit = Retrofit.Builder()   // Retrofit 구성
            .baseUrl(BASE_URL)
            .client(OkHttpClient.Builder().build())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        val api = retrofit.create(KakaoAPI::class.java)   // 통신 인터페이스를 객체로 생성
        val call = api.getSearchKeyword(API_KEY, keyword ,dLatitute.toBigDecimal().toPlainString(), dLongitute.toBigDecimal().toPlainString(), 20000)   // 검색 조건 입력
        Log.d(
            "Test",
            String.format(
                "!!!!!!!!!!MapView onCurrentLocationUpdate (%f,%f)",
                dLatitute,
                dLongitute
            )
        )
        Log.d(
            "Test", dLatitute.toBigDecimal().toPlainString() + dLongitute.toBigDecimal().toPlainString()
        )
        // API 서버에 요청
        call.enqueue(object: Callback<ResultSearchKeyword> {
            override fun onResponse(
                call: Call<ResultSearchKeyword>,
                response: Response<ResultSearchKeyword>
            ) {
                Log.d("Test", "Raw: ${response.raw()}")
                Log.d("Test", "Body: ${response.body()}")
                addItemsAndMarkers(response.body())
            }

            override fun onFailure(call: Call<ResultSearchKeyword>, t: Throwable) {
                Log.w("Test", "onFailure: ${t.message}")
            }
        })
    }

    // 검색 결과 처리 함수
    private fun addItemsAndMarkers(searchResult: ResultSearchKeyword?) {
        if (!searchResult?.documents.isNullOrEmpty()) {
            listItems.clear()
            for (document in searchResult!!.documents) {
                val item = ListLayout(document.place_name,
                    document.address_name,
                    document.road_address_name,
                    document.x.toDouble(),
                    document.y.toDouble())
                listItems.add(item)
            }
            showMarkDetails(0)
        } else {
            Toast.makeText(this, "검색 결과가 없습니다", Toast.LENGTH_SHORT).show()
        }
        goToPoint(dLatitute, dLongitute)
    }
    fun showMarkDetails(count : Int) {
        val size: Int = listItems.size
        var first: MutableList<ListLayout> = ArrayList()
        var subList: MutableList<ListLayout> = ArrayList()

        if(listItems!=null) {
            first = listItems.subList(0, (size + 1) / 2)
            when(count) {
                0 -> subList = first
                1 -> subList = listItems
            }
            mapView?.removeAllPOIItems()
            for (document in subList) {
                val adapter = ListViewAdpater(this, subList)
                binding.listViewTest.adapter = adapter
                // add poi to map
                val marker = MapPOIItem()
                marker.setItemName(document.name);
                marker.setTag(0);
                marker.setMapPoint(
                    MapPoint.mapPointWithGeoCoord(
                        document.y.toDouble(),
                        document.x.toDouble()
                    )
                )
                marker.setMarkerType(MapPOIItem.MarkerType.BluePin)
                marker.setSelectedMarkerType(MapPOIItem.MarkerType.RedPin)

                mapView?.addPOIItem(marker)
            }
        }
    }
    override fun onDestroy() {
        super.onDestroy()
        val mapViewContainer = findViewById<View>(R.id.map_view) as ViewGroup
        mapViewContainer.removeAllViews()
    }

    override fun onCurrentLocationUpdate(mapView: MapView?, currentLocation: MapPoint, accuracyInMeters: Float) {
        val mapPointGeo = currentLocation.mapPointGeoCoord
        dGpsLatitute = mapPointGeo.latitude
        dGpsLongitute = mapPointGeo.longitude
        Log.d(
            "Test",
            String.format(
                "MapView onCurrentLocationUpdate (%f,%f) accuracy (%f)",
                mapPointGeo.latitude,
                mapPointGeo.longitude,
                accuracyInMeters
            )
        )
        if(beforeLoad) {
            mapView!!.setCurrentLocationRadius(20)
            mapView!!.setCurrentLocationRadiusStrokeColor(Color.RED)
            mapView!!.setCurrentLocationRadiusFillColor(Color.RED)
        }
    }

    override fun onCurrentLocationDeviceHeadingUpdate(mapView: MapView?, v: Float) {}

    override fun onCurrentLocationUpdateFailed(mapView: MapView?) {
        Log.d("Test", "onCurrentLocationUpdateFailed");
    }

    override fun onCurrentLocationUpdateCancelled(mapView: MapView?) {
        Log.d("Test", "onCurrentLocationUpdateCancelled");
    }


    private fun onFinishReverseGeoCoding(result: String) {}

    // ActivityCompat.requestPermissions를 사용한 퍼미션 요청의 결과를 리턴받는 메소드
    override fun onRequestPermissionsResult(
    permsRequestCode: Int,
    permissions: Array<String?>,
    grandResults: IntArray
    ) {
        super.onRequestPermissionsResult(permsRequestCode, permissions, grandResults)
        val preference = getPreferences(MODE_PRIVATE)
        var check_result = preference.getBoolean("isFirstPermissionCheck", true)
        if (permsRequestCode == PERMISSIONS_REQUEST_CODE && grandResults.size == REQUIRED_PERMISSIONS.size) {
            // 요청 코드가 PERMISSIONS_REQUEST_CODE 이고, 요청한 퍼미션 개수만큼 수신되었다면
            // 모든 퍼미션을 허용했는지 체크합니다.
            for (result: Int in grandResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    check_result = false
                    break
                }
            }
            if (check_result) {
                Log.d("Test", "start")
                //위치 값을 가져올 수 있음
            } else {
                // 거부한 퍼미션이 있다면 앱을 사용할 수 없는 이유를 설명해주고 앱을 종료합니다.2 가지 경우가 있다
                if (ActivityCompat.shouldShowRequestPermissionRationale(
                        this,
                        REQUIRED_PERMISSIONS.get(0)
                    )
                ) {
                    Toast.makeText(
                        this@MainActivity,
                        "퍼미션이 거부되었습니다. 앱을 다시 실행하여 퍼미션을 허용해주세요.",
                        Toast.LENGTH_LONG
                    ).show()
                    finish()
                } else {
                    Toast.makeText(
                        this@MainActivity,
                        "퍼미션이 거부되었습니다. 설정(앱 정보)에서 퍼미션을 허용해야 합니다. ",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    fun checkRunTimePermission() {
        Log.d("Test", "checkRunTimePermission")
        //런타임 퍼미션 처리
        // 1. 위치 퍼미션을 가지고 있는지 체크합니다.
        val hasFineLocationPermission = ContextCompat.checkSelfPermission(
            this@MainActivity,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
        if (hasFineLocationPermission == PackageManager.PERMISSION_GRANTED) {
            // 2. 이미 퍼미션을 가지고 있다면
            // ( 안드로이드 6.0 이하 버전은 런타임 퍼미션이 필요없기 때문에 이미 허용된 걸로 인식합니다.)
            // 3.  위치 값을 가져올 수 있음
        } else {  //2. 퍼미션 요청을 허용한 적이 없다면 퍼미션 요청이 필요합니다. 2가지 경우(3-1, 4-1)가 있습니다.
            // 3-1. 사용자가 퍼미션 거부를 한 적이 있는 경우에는
            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    this@MainActivity,
                    REQUIRED_PERMISSIONS.get(0)
                )
            ) {
                // 3-2. 요청을 진행하기 전에 사용자가에게 퍼미션이 필요한 이유를 설명해줄 필요가 있습니다.
                Toast.makeText(this@MainActivity, "이 앱을 실행하려면 위치 접근 권한이 필요합니다.", Toast.LENGTH_LONG)
                    .show()
                // 3-3. 사용자게에 퍼미션 요청을 합니다. 요청 결과는 onRequestPermissionResult에서 수신됩니다.
                ActivityCompat.requestPermissions(
                    this@MainActivity, REQUIRED_PERMISSIONS,
                    PERMISSIONS_REQUEST_CODE
                )
            } else {
                // 4-1. 사용자가 퍼미션 거부를 한 적이 없는 경우에는 퍼미션 요청을 바로 합니다.
                // 요청 결과는 onRequestPermissionResult에서 수신됩니다.
                ActivityCompat.requestPermissions(
                    this@MainActivity, REQUIRED_PERMISSIONS,
                    PERMISSIONS_REQUEST_CODE
                )
            }
        }
    }

    private fun showDialogForLocationServiceSetting() {
        val builder = AlertDialog.Builder(this@MainActivity)
        builder.setTitle("위치 서비스 비활성화")
        builder.setMessage(
            "앱을 사용하기 위해서는 위치 서비스가 필요합니다.\n"
                    + "위치 설정을 수정하시겠습니까?"
        )
        builder.setCancelable(true)
        builder.setPositiveButton("설정") { dialog, id ->
            val callGPSSettingIntent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            startActivityForResult(callGPSSettingIntent, GPS_ENABLE_REQUEST_CODE)
        }
        builder.setNegativeButton(
            "취소"
        ) { dialog, id -> dialog.cancel() }
        builder.create().show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            GPS_ENABLE_REQUEST_CODE ->                 //사용자가 GPS 활성 시켰는지 검사
                if (checkLocationServicesStatus()) {
                    if (checkLocationServicesStatus()) {
                        Log.d("Test", "onActivityResult : GPS 활성화 되있음")
                        checkRunTimePermission()
                        return
                    }
                }
        }
    }

    fun checkLocationServicesStatus(): Boolean {
        val locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        return (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER))
    }

    override fun onMapViewInitialized(mapView: MapView?) {}

    override fun onMapViewCenterPointMoved(mapView: MapView?, mapPoint: MapPoint?) {}

    override fun onMapViewZoomLevelChanged(mapView: MapView?, i: Int) {
        if(beforeLoad){
            Log.d("Test", "onMapViewZoomLevelChanged : [beforeClick] do not enable refresh button")
        } else {
            Log.d("Test", "onMapViewZoomLevelChanged : enable refresh button")
            var refresh_btn: Button = findViewById<Button>(R.id.bRefresh)
            refresh_btn.setVisibility(View.VISIBLE)
        }
    }

    override fun onMapViewSingleTapped(mapView: MapView?, mapPoint: MapPoint?) {
        Log.d("Test", "onMapViewSingleTapped");
    }

    override fun onMapViewDoubleTapped(mapView: MapView?, mapPoint: MapPoint?) {
        Log.d("Test", "onMapViewDoubleTapped");
    }

    override fun onMapViewLongPressed(mapView: MapView?, mapPoint: MapPoint?) {
        Log.d("Test", "onMapViewLongPressed");
    }

    override fun onMapViewDragStarted(mapView: MapView?, mapPoint: MapPoint?) {
        if(beforeLoad){
            Log.d("Test", "onMapViewDragStarted : [beforeClick] do not enable refresh button")
        } else {
            Log.d("Test", "onMapViewDragStarted : enable refresh button")
            var refresh_btn: Button = findViewById<Button>(R.id.bRefresh)
            refresh_btn.setVisibility(View.VISIBLE)
        }
    }

    override fun onMapViewDragEnded(mapView: MapView?, mapPoint: MapPoint?) {
        Log.d("Test", "onMapViewDragEnded");
    }

    override fun onMapViewMoveFinished(mapView: MapView?, mapPoint: MapPoint?) {
        Log.d("Test", "onMapViewMoveFinished");
    }

    override fun onPOIItemSelected(mapView: MapView?, poiItem: MapPOIItem?) {
        Log.d("Test", "onPOIItemSelected");
        if (poiItem != null) {
            dLatitute = poiItem.mapPoint.mapPointGeoCoord.latitude
            dLongitute = poiItem.mapPoint.mapPointGeoCoord.longitude
        }
        goToClickPoint(dLatitute, dLongitute)
    }

    override fun onCalloutBalloonOfPOIItemTouched(mapView: MapView?, poiItem: MapPOIItem?) {
        Log.d("Test", "onCalloutBalloonOfPOIItemTouched");
    }

    override fun onCalloutBalloonOfPOIItemTouched(mapView: MapView?, poiItem: MapPOIItem?, buttonType: MapPOIItem.CalloutBalloonButtonType?) {
        Log.d("Test", "onCalloutBalloonOfPOIItemTouched");
    }

    override fun onDraggablePOIItemMoved(mapView: MapView?, poiItem: MapPOIItem?, mapPoint: MapPoint?) {
        // 마커의 속성 중 isDraggable = true 일 때 마커를 이동시켰을 경우
    }
}
//
//private fun MapView?.setPOIItemEventListener(mainActivity: MainActivity) {
//
//}
//
//class MarkerEventListener(val context: Context): MapView.POIItemEventListener {
//    override fun onPOIItemSelected(mapView: MapView?, poiItem: MapPOIItem?) {
//        Log.d("Test", "onPOIItemSelected");
//    }
//
//    override fun onCalloutBalloonOfPOIItemTouched(mapView: MapView?, poiItem: MapPOIItem?) {
//        // 말풍선 클릭 시 (Deprecated)
//        // 이 함수도 작동하지만 그냥 아래 있는 함수에 작성하자
//        Log.d("Test", "1111111onCalloutBalloonOfPOIItemTouched");
//    }
//
//    override fun onCalloutBalloonOfPOIItemTouched(mapView: MapView?, poiItem: MapPOIItem?, buttonType: MapPOIItem.CalloutBalloonButtonType?) {
//        Log.d("Test", "2222222onCalloutBalloonOfPOIItemTouched");
//    }
//
//    override fun onDraggablePOIItemMoved(mapView: MapView?, poiItem: MapPOIItem?, mapPoint: MapPoint?) {
//        // 마커의 속성 중 isDraggable = true 일 때 마커를 이동시켰을 경우
//    }
//}
