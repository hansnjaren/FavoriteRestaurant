package com.example.favoriterestaurant.ui.notifications

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
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
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.*
import com.google.android.libraries.places.api.net.*

class NotificationsFragment : Fragment(), OnMapReadyCallback {

    private var _binding: FragmentNotificationsBinding? = null
    private val binding get() = _binding!!

    private var googleMap: GoogleMap? = null

    private lateinit var placesClient: PlacesClient // 추가

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNotificationsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        //
        if (!Places.isInitialized()) {
            Places.initialize(requireContext(), getString(R.string.google_maps_key), Locale.getDefault())
        }
        placesClient = Places.createClient(requireContext())
        //


        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as? SupportMapFragment
        mapFragment?.getMapAsync(this)
    }



    override fun onMapReady(map: GoogleMap) {
        googleMap = map

        val dgist = LatLng(35.7000, 128.4667)
        googleMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(dgist, 15f))
        googleMap?.addMarker(MarkerOptions().position(dgist).title("DGIST"))

        // POI 클릭 시 placeId로 상세 정보 가져오기
        googleMap?.setOnPoiClickListener { poi ->
            val placeFields = listOf(
                Place.Field.ID,
                Place.Field.NAME,
                Place.Field.ADDRESS,
                Place.Field.PHONE_NUMBER,
                Place.Field.PHOTO_METADATAS
            )

            val request = FetchPlaceRequest.builder(poi.placeId, placeFields).build()

            placesClient.fetchPlace(request)
                .addOnSuccessListener { response ->
                    val place = response.place
                    val photoMetadata = place.photoMetadatas?.firstOrNull()

                    if (photoMetadata != null) {
                        val photoRequest = FetchPhotoRequest.builder(photoMetadata)
                            .setMaxWidth(600)
                            .setMaxHeight(400)
                            .build()

                        placesClient.fetchPhoto(photoRequest)
                            .addOnSuccessListener { photoResponse ->
                                showPlaceDialog(place, photoResponse.bitmap)
                            }
                            .addOnFailureListener {
                                showPlaceDialog(place, null)
                            }
                    } else {
                        showPlaceDialog(place, null)
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("Places", "Fetch 실패: ${e.message}")
                }
        }
    }

    private fun showPlaceDialog(place: Place, bitmap: android.graphics.Bitmap?) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_place_info, null)

        val image = dialogView.findViewById<ImageView>(R.id.place_image)
        val name = dialogView.findViewById<TextView>(R.id.place_name)
        val address = dialogView.findViewById<TextView>(R.id.place_address)
        val phone = dialogView.findViewById<TextView>(R.id.place_phone)

        name.text = place.name ?: "이름 없음"
        address.text = place.address ?: "주소 없음"
        phone.text = place.phoneNumber ?: "전화번호 없음"

        if (bitmap != null) {
            image.setImageBitmap(bitmap)
        } else {
            image.setImageResource(android.R.drawable.ic_menu_report_image)
            // image.setImageResource(R.drawable.no_image) // 기본 이미지
        }

        AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setPositiveButton("확인", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        googleMap = null
    }
}
