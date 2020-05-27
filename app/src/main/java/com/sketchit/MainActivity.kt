package com.sketchit

import android.Manifest
import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.media.MediaScannerConnection
import android.os.AsyncTask
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.get
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.dialog_brush_size.*
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.lang.Exception

class MainActivity : AppCompatActivity() {

    private var mImageButtonCurrentPaint : ImageButton? =null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        drawing_view.setSizeForBrush(20.toFloat()) // Setting the default brush size to drawing view.

        mImageButtonCurrentPaint = ll_paint_colors[1] as ImageButton
        mImageButtonCurrentPaint!!.setImageDrawable(
            ContextCompat.getDrawable(this,R.drawable.pallet_selected)
        )

        ib_brush.setOnClickListener {
            showBrushSizeChooserDialog()
        }

        ib_gallery.setOnClickListener {
            if(isReadStorageAllowed()) {

                val pickPhotoIntent = Intent(Intent.ACTION_PICK,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI)

                startActivityForResult(pickPhotoIntent,GALLERY)
            }
            else{
                requestStoragePermission()
            }
        }

    ib_undo.setOnClickListener {
        drawing_view.onClickUndo()
    }

        ib_save.setOnClickListener {
            if(isReadStorageAllowed()) {
                BitmapASyncTask(getBitmapFromView(fl_drawing_view_container)).execute()
            }
            else{
                requestStoragePermission()
            }
        }

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(resultCode == Activity.RESULT_OK) {
            if(requestCode == GALLERY)
            {
                try {
                       if(data!!.data !=null) {
                           iv_background.visibility =View.VISIBLE
                           iv_background.setImageURI(data.data)
                       }
                    else{
                           Toast.makeText(this,
                               "Error in Parsing the Image or its Corrupted",
                               Toast.LENGTH_SHORT).show()
                       }
                }catch (e: Exception){
                    e.printStackTrace()
                }
            }
        }
    }

    private  fun showBrushSizeChooserDialog() {
        val brushDialog = Dialog(this)
        brushDialog.setContentView(R.layout.dialog_brush_size)
        brushDialog.setTitle("Brush size: ")
        val extraSmallBtn = brushDialog.ib_extrasmall_brush
        extraSmallBtn.setOnClickListener{
            drawing_view.setSizeForBrush(4.toFloat())
            brushDialog.dismiss()
        }

        val smallBtn = brushDialog.ib_small_brush
        smallBtn.setOnClickListener{
            drawing_view.setSizeForBrush(6.toFloat())
            brushDialog.dismiss()
        }

        val mediumBtn = brushDialog.ib_medium_brush
        mediumBtn.setOnClickListener{
            drawing_view.setSizeForBrush(9.toFloat())
            brushDialog.dismiss()
        }

        val largeBtn = brushDialog.ib_large_brush
        largeBtn.setOnClickListener{
            drawing_view.setSizeForBrush(15.toFloat())
            brushDialog.dismiss()
        }

        val extralargeBtn = brushDialog.ib_extralarge_brush
        extralargeBtn.setOnClickListener{
            drawing_view.setSizeForBrush(19.toFloat())
            brushDialog.dismiss()
        }

        brushDialog.show()
    }

    fun paintClicked(view :View) {
        if(view!==mImageButtonCurrentPaint) {
            val imageButton = view as ImageButton

            val colorTag = imageButton.tag.toString()
            drawing_view.setColor(colorTag)

            imageButton.setImageDrawable(
                ContextCompat.getDrawable(this,R.drawable.pallet_selected)
            )
            mImageButtonCurrentPaint!!.setImageDrawable(
                ContextCompat.getDrawable(this,R.drawable.pallet_normal)
            )
            mImageButtonCurrentPaint = view
        }
    }

    private fun requestStoragePermission() {
        if(ActivityCompat.shouldShowRequestPermissionRationale(this,
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE).toString()))
        {
            Toast.makeText(this,"Need permission to add a background",
            Toast.LENGTH_SHORT).show()
        }
        ActivityCompat.requestPermissions(this,arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE),
            STORAGE_PERMISSION_CODE)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if(requestCode == STORAGE_PERMISSION_CODE) {

            if(grantResults.isNotEmpty() && grantResults[0]
                == PackageManager.PERMISSION_GRANTED) {

                Toast.makeText(this,
                "permission granted you can read storage",
                Toast.LENGTH_LONG).show()
            }
            else
            {
                Toast.makeText(this,
                    "opps you Declined the permission",
                    Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun isReadStorageAllowed() : Boolean {
        val result = ContextCompat.checkSelfPermission(this,Manifest.permission.READ_EXTERNAL_STORAGE)
        return result == PackageManager.PERMISSION_GRANTED

    }

    private fun getBitmapFromView(view: View) :Bitmap{
        val returnedBitmap = Bitmap.createBitmap(view.width,
            view.height,Bitmap.Config.ARGB_8888)
        val canvas = Canvas(returnedBitmap)
        val bgDrawable = view.background

        if(bgDrawable!=null) {
            bgDrawable.draw(canvas)
        }
        else{
            canvas.drawColor(Color.WHITE)
        }

        view.draw(canvas)
        return returnedBitmap
    }

    private inner class BitmapASyncTask(val mBitmap:Bitmap) :
        AsyncTask<Any,Void,String>(){

        private lateinit var mProgressDialog : Dialog

        override fun onPreExecute() {
            super.onPreExecute()
            showProgressDialog()
        }

        override fun doInBackground(vararg params: Any?): String {
            var result =""
            if(mBitmap!=null) {
                try {
                      val bytes = ByteArrayOutputStream()
                     mBitmap.compress(Bitmap.CompressFormat.PNG,90,bytes)
                    val f = File(externalCacheDir!!.absoluteFile.toString()
                            + File.separator + "Sketchit_"
                            + System.currentTimeMillis() /1000 + ".png")
                    val fos = FileOutputStream(f)
                    fos.write((bytes.toByteArray()))
                    fos.close()
                    return f.absolutePath
                }catch (e: Exception) {
                    result=""
                    e.printStackTrace()
                }
            }
            return result
        }

        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)

            cancelProgressDialog()

            if(!result!!.isEmpty()) {
                Toast.makeText(
                    this@MainActivity,
                    "File Saved Succeefully : $result",
                    Toast.LENGTH_LONG).show()
            }
            else{
                Toast.makeText(
                    this@MainActivity,
                    "Something Went Wrong While Saving the File",
                    Toast.LENGTH_LONG).show()
            }

        }

        private fun showProgressDialog(){
             mProgressDialog = Dialog(this@MainActivity)
            mProgressDialog.setContentView(R.layout.dialog_custom_progress)
            mProgressDialog.show()
        }

        private fun cancelProgressDialog() {
            mProgressDialog.dismiss()
        }

    }

    companion object{
        private const val STORAGE_PERMISSION_CODE = 1
        private const val GALLERY =2
    }
}