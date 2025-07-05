package com.example.favoriterestaurant.ui.notifications

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.favoriterestaurant.databinding.FragmentNotificationsBinding
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import android.app.AlertDialog
import android.location.Geocoder
import com.example.favoriterestaurant.R
import java.util.Locale

class NotificationsFragment : Fragment(), OnMapReadyCallback {

    private var _binding: FragmentNotificationsBinding? = null
    private val binding get() = _binding!!

    private var googleMap: GoogleMap? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNotificationsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as? SupportMapFragment
        mapFragment?.getMapAsync(this)
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        val dgist = LatLng(35.7000, 128.4667)

        try {
            googleMap?.addMarker(MarkerOptions().position(dgist).title("Marker at DGIST"))
            googleMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(dgist, 15f))

            // 지도 클릭 시 위치 정보 표시
            googleMap?.setOnMapClickListener { latLng ->
                val geocoder = Geocoder(requireContext(), Locale.getDefault())
                val addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)

                if (!addresses.isNullOrEmpty()) {
                    val address = addresses[0].getAddressLine(0)  // 전체 주소
                    val placeName = addresses[0].featureName       // 장소 이름 (건물명 등)

                    // 팝업으로 주소 보여주기
                    AlertDialog.Builder(requireContext())
                        .setTitle(placeName ?: "알 수 없는 장소")
                        .setMessage(address ?: "주소 정보를 찾을 수 없습니다.")
                        .setPositiveButton("확인", null)
                        .show()
                }
            }

        } catch (e: Exception) {
            Log.e("MapError", "Error adding marker: ${e.message}")
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        googleMap = null
    }
}
