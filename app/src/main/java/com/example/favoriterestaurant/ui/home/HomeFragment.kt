package com.example.favoriterestaurant.ui.home

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.favoriterestaurant.R
import com.example.favoriterestaurant.databinding.FragmentHomeBinding
import com.example.favoriterestaurant.utils.DialogUtils
import com.example.favoriterestaurant.utils.ImageItem
import com.example.favoriterestaurant.utils.getImageItemListFlow
import com.example.favoriterestaurant.utils.imageList
import com.example.favoriterestaurant.utils.saveImageItemList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class DataAdapter(
    private val context: Context,
    private val imageList: MutableList<ImageItem>
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val nameView: TextView = view.findViewById(R.id.nameView)
        val descView: TextView = view.findViewById(R.id.descView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_layout, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int {
        return imageList.size
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val listHolder = holder as ViewHolder
        val imageData = imageList[position]
        val name = imageData.name
        val desc = imageData.desc
        listHolder.nameView.text = name
        listHolder.descView.text = desc
        listHolder.itemView.setOnClickListener {
            DialogUtils.showImageDialog(
                context = listHolder.itemView.context,
                imageData = imageData,
                imageList = imageList,
                submitList = { newList -> submitList(newList) },
                releasePermission = { uri ->
                    try {
                        listHolder.itemView.context.contentResolver.releasePersistableUriPermission(
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