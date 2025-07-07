package com.example.favoriterestaurant.utils

import android.app.AlertDialog
import android.content.Context
import android.net.Uri
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.favoriterestaurant.R
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.lang.reflect.Type
import androidx.core.net.toUri
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.gson.JsonNull
import com.google.gson.JsonObject

val imageList: MutableList<ImageItem> = mutableListOf()

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "image_items")

data class ImageItem(
    var order: Int,
    var visible: Boolean,
    val uri: Uri,
    var name: String,
    var desc: String,
    var address: String?,
    var phoneNumber: String?,
    var coord: LatLng?,
    @Transient var marker: Marker?
)

val IMAGE_LIST_KEY = stringPreferencesKey("image_list")

class UriAdapter : JsonSerializer<Uri>, JsonDeserializer<Uri> {
    override fun serialize(
        src: Uri?,
        typeOfSrc: Type?,
        context: JsonSerializationContext?
    ): JsonElement {
        return JsonPrimitive(src?.toString())
    }

    override fun deserialize(
        json: JsonElement?,
        typeOfT: Type?,
        context: JsonDeserializationContext?
    ): Uri {
        return json?.asString!!.toUri()
    }
}

class LatLngAdapter : JsonSerializer<LatLng>, JsonDeserializer<LatLng> {
    override fun serialize(
        src: LatLng?,
        typeOfSrc: Type?,
        context: JsonSerializationContext?
    ): JsonElement {
        return if (src != null) {
            JsonObject().apply {
                addProperty("lat", src.latitude)
                addProperty("lng", src.longitude)
            }
        } else {
            JsonNull.INSTANCE
        }
    }

    override fun deserialize(
        json: JsonElement?,
        typeOfT: Type?,
        context: JsonDeserializationContext?
    ): LatLng? {
        if (json == null || json.isJsonNull) return null
        val obj = json.asJsonObject
        val latElement = obj.get("lat")
        val lngElement = obj.get("lng")
        val lat = if (latElement != null && !latElement.isJsonNull) latElement.asDouble else null
        val lng = if (lngElement != null && !lngElement.isJsonNull) lngElement.asDouble else null
        return if (lat != null && lng != null) LatLng(lat, lng) else null
    }

}

val gson: Gson = GsonBuilder()
    .registerTypeAdapter(Uri::class.java, UriAdapter())
    .registerTypeAdapter(LatLng::class.java, LatLngAdapter())
    .create()

suspend fun saveImageItemList(context: Context, itemList: List<ImageItem>) {
    val json = gson.toJson(itemList)
    context.dataStore.edit { prefs ->
        prefs[IMAGE_LIST_KEY] = json
    }
}

fun getImageItemListFlow(context: Context): Flow<List<ImageItem>> {
    return context.dataStore.data.map { prefs ->
        val json = prefs[IMAGE_LIST_KEY] ?: "[]"
        val type = object : TypeToken<List<ImageItem>>() {}.type
        gson.fromJson(json, type)
    }
}

object DialogUtils {
    fun showImageDialog(
        context: Context,
        imageData: ImageItem,
        imageList: MutableList<ImageItem>,
        submitList: (List<ImageItem>) -> Unit,
        releasePermission: (Uri) -> Unit
    ) {
        val inflater = LayoutInflater.from(context)
        val dialogView = inflater.inflate(R.layout.alert_with_image, null)
        val imageView = dialogView.findViewById<ImageView>(R.id.dialogImage)
        val nameEditor = dialogView.findViewById<EditText>(R.id.editName)
        val descEditor = dialogView.findViewById<EditText>(R.id.editDesc)
        val addressView = dialogView.findViewById<TextView>(R.id.addressView)
        val phoneNumberView = dialogView.findViewById<TextView>(R.id.phoneNumberView)
        imageView.setImageURI(imageData.uri)
        nameEditor.setText(imageData.name)
        descEditor.setText(imageData.desc)
        if (imageData.address != null) {
            addressView.text = imageData.address
        }
        if (imageData.phoneNumber != null) {
            phoneNumberView.text = imageData.phoneNumber
        }

        var shouldReleasePermission = false

        val dialog = AlertDialog.Builder(context)
            .setView(dialogView)
            .setPositiveButton("확인") { _, _ ->
                var nameInput = nameEditor.text.toString()
                var descInput = descEditor.text.toString()
                if (nameInput.isEmpty()) nameInput = "No name"
                if (descInput.isEmpty()) descInput = "No description"
                imageData.name = nameInput
                imageData.desc = descInput
                val newList = imageList.toMutableList()
                imageList.clear()
                imageList.addAll(newList)
                submitList(newList)
            }
            .setNegativeButton("삭제") { _, _ ->
                val index = imageList.indexOf(imageData)
                if (index != -1) {
                    val newList = imageList.toMutableList().apply { removeAt(index) }
                    imageList.clear()
                    imageList.addAll(newList)
                    submitList(newList)
                    shouldReleasePermission = true
                }
            }
            .create()

        dialog.setOnDismissListener {
            if (shouldReleasePermission) {
                releasePermission(imageData.uri)
                shouldReleasePermission = false
            }
        }
        dialog.show()
    }
}
