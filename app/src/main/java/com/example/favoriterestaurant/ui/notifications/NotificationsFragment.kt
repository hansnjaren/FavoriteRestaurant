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
import android.content.pm.PackageManager
import android.content.res.Resources
import androidx.lifecycle.lifecycleScope
import com.example.favoriterestaurant.R
import com.example.favoriterestaurant.utils.ImageItem
import com.example.favoriterestaurant.utils.imageList
import com.example.favoriterestaurant.utils.saveImageItemList
import com.google.android.gms.maps.model.LatLngBounds
import kotlinx.coroutines.launch
import java.util.Locale
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.*
import com.google.android.libraries.places.api.net.*
import android.widget.EditText
import android.widget.Button



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

        val context = requireContext()
        val appInfo = context.packageManager.getApplicationInfo(
            context.packageName,
            PackageManager.GET_META_DATA
        )
        val apiKey = appInfo.metaData.getString("com.google.android.geo.API_KEY")

        //place api 초기화
        if (!Places.isInitialized()) {
            Places.initialize(
                requireContext(),
                apiKey!!,
                Locale.getDefault()
            )
        }
        placesClient = Places.createClient(requireContext())
        //지도 초기화
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as? SupportMapFragment
        mapFragment?.getMapAsync(this)

        //검색창 초기화
        val searchInput = view.findViewById<EditText>(R.id.search_input)
        val searchButton = view.findViewById<Button>(R.id.search_button)

        searchButton.setOnClickListener {
            val query = searchInput.text.toString()
            if (query.isNotBlank()) {
                searchPlace(query)
            }
        }



    }

    //검색기능 추가
    private fun searchPlace(query: String) {
        val request = FindAutocompletePredictionsRequest.builder()
            .setQuery(query)
            .build()

        placesClient.findAutocompletePredictions(request)
            .addOnSuccessListener { response ->
                if (response.autocompletePredictions.isNotEmpty()) {
                    val prediction = response.autocompletePredictions[0]
                    val placeId = prediction.placeId

                    val placeFields = listOf(
                        Place.Field.ID,
                        Place.Field.DISPLAY_NAME,
                        Place.Field.FORMATTED_ADDRESS,
                        Place.Field.LOCATION,
                        Place.Field.PHOTO_METADATAS,
                        Place.Field.INTERNATIONAL_PHONE_NUMBER
                    )

                    val fetchPlaceRequest = FetchPlaceRequest.builder(placeId, placeFields).build()

                    //  코루틴 비동기 처리 권장
                    placesClient.fetchPlace(fetchPlaceRequest)
                        .addOnSuccessListener { fetchResponse ->
                            val place = fetchResponse.place

                            // 사진 가져오기도 코루틴이나 background에서 실행
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
                        .addOnFailureListener {
                            Log.e("Search", "장소 정보 가져오기 실패: ${it.message}")
                        }
                }
            }
            .addOnFailureListener {
                Log.e("Search", "자동완성 실패: ${it.message}")
            }
    }


    override fun onMapReady(map: GoogleMap) {
        googleMap = map

        val builder = LatLngBounds.Builder()
        var counter = 0

        for (item in imageList) {
            if (item.coord != null) {
                val marker =
                    googleMap?.addMarker(MarkerOptions().position(item.coord!!).title(item.name))
                item.marker = marker
                marker?.tag = item
                builder.include(item.coord!!)
                counter++
            }
        }

        if (counter > 0) {
            val bounds = builder.build()
            if (counter == 1) {
                googleMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(bounds.center, 15f))
            }
            else {
                val displayMetrics = Resources.getSystem().displayMetrics
                val width = displayMetrics.widthPixels
                val padding = (width * 0.1).toInt()

                googleMap?.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, padding))
            }
        }
        else {
            val dgist = LatLng(35.7000, 128.4667)
            googleMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(dgist, 15f))
        }

        googleMap?.setOnPoiClickListener { poi ->
            val placeFields = listOf(
                Place.Field.ID,
                Place.Field.DISPLAY_NAME,
                Place.Field.FORMATTED_ADDRESS,
                Place.Field.INTERNATIONAL_PHONE_NUMBER,
                Place.Field.PHOTO_METADATAS,
                Place.Field.LOCATION
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

        googleMap?.setOnMarkerClickListener { marker ->
            val imageData = marker.tag as ImageItem

            val inflater = LayoutInflater.from(context)
            val dialogView = inflater.inflate(R.layout.map_alert, null)
            val imageView = dialogView.findViewById<ImageView>(R.id.mapDialogImage)
            val nameView = dialogView.findViewById<TextView>(R.id.mapName)
            val descView = dialogView.findViewById<TextView>(R.id.mapDesc)
            val addressView = dialogView.findViewById<TextView>(R.id.mapAddressView)
            val phoneNumberView = dialogView.findViewById<TextView>(R.id.mapPhoneNumberView)
            imageView.setImageURI(imageData.uri)
            nameView.text = imageData.name
            descView.text = imageData.desc
            if (imageData.address != null) {
                addressView.text = imageData.address
            }
            if (imageData.phoneNumber != null) {
                phoneNumberView.text = imageData.phoneNumber
            }

            AlertDialog.Builder(context)
                .setView(dialogView)
                .setPositiveButton("확인", null)
                .show()
            true
        }
    }

    private fun showPlaceDialog(place: Place, bitmap: android.graphics.Bitmap?) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_place_info, null)

        val imageView = dialogView.findViewById<ImageView>(R.id.place_image)
        val nameView = dialogView.findViewById<TextView>(R.id.place_name)
        val addressView = dialogView.findViewById<TextView>(R.id.place_address)
        val phoneView = dialogView.findViewById<TextView>(R.id.place_phone)

        val name = place.displayName ?: "이름 없음"
        val address = place.formattedAddress ?: "주소 없음"
        val phone = place.internationalPhoneNumber ?: "전화번호 없음"

        nameView.text = name
        addressView.text = address
        phoneView.text = phone

        if (bitmap != null) {
            imageView.setImageBitmap(bitmap)
        } else {
            imageView.setImageResource(android.R.drawable.ic_menu_report_image)
            // image.setImageResource(R.drawable.no_image) // 기본 이미지
        }

        AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setPositiveButton("확인", null)
            .setNegativeButton("추가") { _, _ ->
                AlertDialog.Builder(context)
                    .setTitle("추가하기")
                    .setItems(imageList.map { it.name }.toTypedArray()) { _, which ->
                        val newMarker = googleMap?.addMarker(
                            MarkerOptions()
                                .position(place.location!!)
                                .title(imageList[which].name)
                        )

                        if (imageList[which].marker != null) {
                            imageList[which].marker!!.remove()
                        }
                        imageList[which].name = name
                        imageList[which].address = address
                        imageList[which].phoneNumber = phone
                        imageList[which].coord = place.location!!
                        imageList[which].marker = newMarker
                        newMarker?.tag = imageList[which]
                        Log.d("marker added", newMarker?.tag.toString())
                        lifecycleScope.launch {
                            saveImageItemList(requireContext(), imageList)
                        }
                    }
                    .setNegativeButton("취소", null)
                    .show()
            }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        googleMap = null
    }
}
