package com.example.favoriterestaurant.ui.dashboard

import android.content.Context
import android.content.Intent
import android.media.Image
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.core.net.toUri
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.favoriterestaurant.R
import com.example.favoriterestaurant.databinding.FragmentDashboardBinding
import androidx.datastore.preferences.preferencesDataStore
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
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
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

//data class MyItem(val title: String, val description: String, val image: String)
//class MyAdapter(private val itemList: List<MyItem>) :
//    RecyclerView.Adapter<MyAdapter.MyViewHolder>() {
//
//    class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
//        val titleView: TextView = itemView.findViewById(R.id.itemTitle)
//        val descView: TextView = itemView.findViewById(R.id.itemDesc)
//        val hardText: TextView = itemView.findViewById(R.id.hardText)
//        val image: ImageView = itemView.findViewById(R.id.imageView)
//    }
//
//    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
//        val view = LayoutInflater.from(parent.context)
//            .inflate(R.layout.item_layout, parent, false)
//        return MyViewHolder(view)
//    }
//
//    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
//        val item = itemList[position]
//        val context = holder.itemView.context
//        holder.titleView.text = item.title
//        holder.descView.text = item.description
//        holder.hardText.text = "Hello, World!"
//
//        holder.image.setImageResource(context.resources.getIdentifier(item.image, "drawable", context.packageName))
//
//        holder.itemView.setOnClickListener {
//            AlertDialog.Builder(holder.itemView.context)
//                .setTitle(item.title)
//                .setMessage("내용: ${item.description}")
//                .setPositiveButton("확인", null)
//                .show()
//        }
//    }
//
//    override fun getItemCount() = itemList.size
//}

val imageList: MutableList<ImageItem> = mutableListOf()

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "image_items")

data class ImageItem(val uri: Uri, val name: String)

val IMAGE_LIST_KEY = stringPreferencesKey("image_list")

class UriAdapter: JsonSerializer<Uri>, JsonDeserializer<Uri> {
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
        return Uri.parse(json?.asString)
    }
}

val gson: Gson = GsonBuilder()
    .registerTypeAdapter(Uri::class.java, UriAdapter())
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
        val type = object: TypeToken<List<ImageItem>>() {}.type
        gson.fromJson<List<ImageItem>>(json, type)
    }
}

class ImageAdapter(
    private val context: Context,
    private val imageList: MutableList<ImageItem>
) : RecyclerView.Adapter<ImageAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageView: ImageView = view.findViewById(R.id.imageView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_image, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val imageData = imageList[position]
        val uri = imageData.uri
        val name = imageData.name
        holder.imageView.setImageURI(uri)


        holder.itemView.setOnClickListener {
            val inflater = LayoutInflater.from(holder.itemView.context)
            val dialogView = inflater.inflate(R.layout.alert_with_image, null)
            val imageView = dialogView.findViewById<ImageView>(R.id.dialogImage)
            imageView.setImageURI(uri)

            var shouldReleasePermission = false

            val dialog = AlertDialog.Builder(holder.itemView.context)
                .setTitle(name)
                .setView(dialogView)
                .setPositiveButton("확인", null)
                .setNegativeButton("삭제") { _, _ ->
                    // 1. 리스트에서 이미지 제거
                    val currentPosition = holder.adapterPosition
                    if (currentPosition != RecyclerView.NO_POSITION) {
                        val newList = imageList.toMutableList().apply { removeAt(currentPosition) }
                        imageList.clear()
                        imageList.addAll(newList)
                        submitList(newList)
                        shouldReleasePermission = true
                    }
                }.create()
            dialog.setOnDismissListener {
                // 2. 권한 반납
                if(shouldReleasePermission) {
                    Log.d("MyTag", "try release permission")
                    try {
                        holder.itemView.context.contentResolver.releasePersistableUriPermission(
                            uri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION
                        )
                    } catch (e: SecurityException) {
                        // 이미 권한이 없거나 예외 발생 시 무시
                        Log.d("MyTag","no permission therefore not released")
                    }
                    shouldReleasePermission = false
                }
            }
            dialog.show()
        }
    }

    override fun getItemCount(): Int = imageList.size

    // 데이터 갱신 메서드
    fun submitList(newItems: List<ImageItem>) {
        val uriList: MutableList<Uri> = mutableListOf()
        for(image in imageList){
            uriList.add(image.uri)
        }
        for(item in newItems){
            if(!uriList.contains(item.uri)){
                imageList.add(item)
            }
        }
        // imageList.addAll(newItems)
        notifyDataSetChanged()
        CoroutineScope(Dispatchers.IO).launch {
            saveImageItemList(context, imageList)
        }
    }
}


//class ImageAdapter : ListAdapter<Uri, ImageAdapter.ViewHolder>(DiffCallback()) {
//
//    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
//        val imageView: ImageView = view.findViewById(R.id.imageView)
//    }
//
//    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
//        val view = LayoutInflater.from(parent.context)
//            .inflate(R.layout.item_image, parent, false)
//        return ViewHolder(view)
//    }
//
//    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
//        val uri = getItem(position)
//        holder.imageView.setImageURI(uri)
//
//        val inflater = LayoutInflater.from(holder.itemView.context)
//        val dialogView = inflater.inflate(R.layout.alert_with_image, null)
//        val imageView = dialogView.findViewById<ImageView>(R.id.dialogImage)
//        imageView.setImageURI(uri)
//
//        holder.itemView.setOnClickListener {
//            AlertDialog.Builder(holder.itemView.context)
//                .setTitle(uri.toString())
//                .setView(dialogView)
//                .setPositiveButton("확인", null)
//                .show()
//        }
//    }
//
//    class DiffCallback : DiffUtil.ItemCallback<Uri>() {
//        override fun areItemsTheSame(oldItem: Uri, newItem: Uri) = oldItem == newItem
//        override fun areContentsTheSame(oldItem: Uri, newItem: Uri) = oldItem == newItem
//    }
//}

class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null

    private lateinit var adapter: ImageAdapter

    private val pickImages = registerForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris ->
        if (uris.isNotEmpty()) {
            val images: MutableList<ImageItem> = mutableListOf()
            for(uri in uris) {
                requireContext().contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
                images.add(ImageItem(uri = uri, name = uris.indexOf(uri).toString()))
            }
            adapter.submitList(images) // RecyclerView에 이미지 URI 리스트 전달
        }
    }

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val recyclerView: RecyclerView = binding.recyclerView

        val buttonSelectImages = root.findViewById<Button>(R.id.buttonSelectImages)

        adapter = ImageAdapter(requireContext(), imageList)
        recyclerView.layoutManager = GridLayoutManager(requireContext(), 2)
        recyclerView.adapter = adapter

        viewLifecycleOwner.lifecycleScope.launch {
            getImageItemListFlow(requireContext()).collect { loadedList ->
                adapter.submitList(loadedList)
            }
        }

        buttonSelectImages.setOnClickListener {
            pickImages.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}