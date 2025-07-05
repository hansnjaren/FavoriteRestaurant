package com.example.favoriterestaurant.ui.notifications

import android.util.Log
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.favoriterestaurant.databinding.FragmentNotificationsBinding
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions

class NotificationsFragment : Fragment(), OnMapReadyCallback {

    private var _binding: FragmentNotificationsBinding? = null
    private val binding get() = _binding!!

    private lateinit var map: GoogleMap

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNotificationsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val mapFragment = childFragmentManager.findFragmentById(com.example.favoriterestaurant.R.id.map) as? SupportMapFragment
        if (mapFragment == null) {
            Log.e("MapDebug", "mapFragment is NULL!!")
        } else {
            Log.d("MapDebug", "mapFragment is ready, calling getMapAsync")
            mapFragment.getMapAsync(this)
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap

        // 원하는 위치로 마커 추가
        val dgist = LatLng(35.7000, 128.4667) // 예시: DGIST
        map.addMarker(MarkerOptions().position(dgist).title("Marker at DGIST"))
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(dgist, 15f))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
