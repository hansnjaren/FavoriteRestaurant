package com.example.favoriterestaurant.ui.home

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract.Data
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ImageView
import android.widget.ListAdapter
import android.widget.TextView
import android.widget.ListView
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.example.favoriterestaurant.R
import com.example.favoriterestaurant.databinding.FragmentHomeBinding
import com.example.favoriterestaurant.ui.dashboard.ImageAdapter
import com.example.favoriterestaurant.ui.dashboard.ImageItem
import com.example.favoriterestaurant.ui.dashboard.getImageItemListFlow
import com.example.favoriterestaurant.ui.dashboard.imageList
import kotlinx.coroutines.launch
import org.w3c.dom.Text
import java.util.ArrayList
import kotlin.collections.MutableList
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.favoriterestaurant.databinding.FragmentDashboardBinding
import com.example.favoriterestaurant.ui.dashboard.ImageAdapter.ViewHolder
import com.example.favoriterestaurant.ui.dashboard.saveImageItemList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

//class HomeFragment : Fragment() {
//
//    private var _binding: FragmentHomeBinding? = null
//
//    // This property is only valid between onCreateView and
//    // onDestroyView.
//    private val binding get() = _binding!!
//
//    override fun onCreateView(
//        inflater: LayoutInflater,
//        container: ViewGroup?,
//        savedInstanceState: Bundle?
//    ): View {
//        _binding = FragmentHomeBinding.inflate(inflater, container, false)
//        val root: View = binding.root
//
//        val listView: ListView = binding.listView
//
//        val itemList: MutableList<String> = mutableListOf()
//
//        for (i in 0 until 30) {
//            itemList.add(i.toString())
//        }
//
//        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, itemList)
//        listView.adapter = adapter
//
//        listView.setOnItemClickListener { parent, view, position, id ->
//            val item: ImageItem = parent.getItemAtPosition(position) as ImageItem
//            val name = item.name
//            val uri = item.uri
////            Toast.makeText(context, "$position 번째 아이템 클릭: $item", Toast.LENGTH_SHORT).show()
//            AlertDialog.Builder(requireContext())
//                .setTitle("Item Click")
//                .setMessage("Chosen: $name, URI: $uri")
//                .setPositiveButton("OK", null)
//                .show()
//        }
//
//
//        return root
//    }
//
//}

class DataAdapter(
    private val context: Context,
    private val imageList: MutableList<ImageItem>
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    companion object {
        const val VIEW_TYPE_TEXT = 0
        const val VIEW_TYPE_IMAGE = 1
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val uriView: TextView = view.findViewById(R.id.uriView)
        val nameView: TextView = view.findViewById(R.id.nameView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_layout, parent, false)
        return DataAdapter.ViewHolder(view)
    }

    override fun getItemCount(): Int {
        return imageList.size
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val holder = holder as DataAdapter.ViewHolder
        val imageData = imageList[position]
        val uri = imageData.uri
        val name = imageData.name
        holder.uriView.text = imageData.uri.toString()
        holder.nameView.text = imageData.name
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

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null

    private lateinit var adapter: DataAdapter
    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val recyclerView: RecyclerView = binding.dataView

        adapter = DataAdapter(requireContext(), imageList)
        recyclerView.layoutManager = GridLayoutManager(requireContext(), 1)
        recyclerView.adapter = adapter

        viewLifecycleOwner.lifecycleScope.launch {
            getImageItemListFlow(requireContext()).collect { loadedList ->
                adapter.submitList(loadedList)
            }
        }

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}