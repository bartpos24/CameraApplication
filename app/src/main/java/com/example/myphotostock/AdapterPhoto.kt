package com.example.myphotostock

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import kotlinx.android.synthetic.main.cell_photo.view.*

class AdapterPhoto(private val context: Context, private val photosList: MutableList<Photo>): BaseAdapter() {

    override fun getCount(): Int {
        return photosList.count()
    }

    override fun getItem(position: Int): Any {
        return photosList[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val photo = this.photosList[position]

        val inflator = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val photoView = inflator.inflate(R.layout.cell_photo, null)
        //photoView.IV_photo.setImageResource(R.drawable.ic_baseline_cloud_download_24)

        //Picasso.get().load(photo.urlToFile).error(R.drawable.ic_baseline_block_24).placeholder(R.drawable.ic_baseline_cloud_download_24).into(photoView.IV_photo)

        Glide.with(context)
            .load(photo.urlToFile)
            .apply(RequestOptions().placeholder(R.drawable.ic_baseline_cloud_download_24).error(R.drawable.ic_baseline_block_24))
            .into(photoView.IV_photo)

        photoView.setOnClickListener {
            val intent = Intent(context, PhotoFromGalleryPreviewActivity::class.java)
            intent.putExtra("urlP", photo.urlToFile)
            intent.putExtra("idAlbumP", photo.albumId)
            intent.putExtra("nameP", photo.photoName)
            intent.flags = Intent.FLAG_ACTIVITY_NO_HISTORY
            context.startActivity(intent)
        }

        return photoView
    }

}