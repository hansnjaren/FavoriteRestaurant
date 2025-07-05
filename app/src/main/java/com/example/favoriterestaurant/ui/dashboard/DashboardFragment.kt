package com.example.favoriterestaurant.ui.dashboard

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.favoriterestaurant.R
import com.example.favoriterestaurant.databinding.FragmentDashboardBinding
import com.example.favoriterestaurant.utils.DialogUtils
import com.example.favoriterestaurant.utils.ImageItem
import com.example.favoriterestaurant.utils.getImageItemListFlow
import com.example.favoriterestaurant.utils.imageList
import com.example.favoriterestaurant.utils.saveImageItemList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


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
        holder.imageView.setImageURI(uri)
        holder.itemView.setOnClickListener {
            DialogUtils.showImageDialog(
                context = holder.itemView.context,
                imageData = imageData,
                imageList = imageList,
                submitList = { newList -> submitList(newList) },
                releasePermission = { uri ->
                    try {
                        holder.itemView.context.contentResolver.releasePersistableUriPermission(
                            uri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION
                        )
                    } catch (e: SecurityException) {
                        Log.d("MyTag", "no permission therefore not released")
                    }
                }
            )
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
        notifyDataSetChanged()
        CoroutineScope(Dispatchers.IO).launch {
            saveImageItemList(context, imageList)
        }
    }
}

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
                images.add(ImageItem(uri = uri, name = "No name", desc = "No description"))
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

        val recyclerView: RecyclerView = binding.imageListView

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