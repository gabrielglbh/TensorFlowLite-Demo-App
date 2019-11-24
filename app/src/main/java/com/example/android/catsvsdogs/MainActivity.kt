package com.example.android.catsvsdogs

import android.Manifest
import android.app.Activity
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import com.squareup.picasso.Picasso
import org.tensorflow.lite.Interpreter
import com.squareup.picasso.Callback
import java.lang.Exception
import android.util.DisplayMetrics
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.os.Environment
import android.provider.MediaStore
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.exifinterface.media.ExifInterface
import com.example.android.catsvsdogs.models.CatModel
import com.example.android.catsvsdogs.models.DogModel
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import retrofit2.Call
import retrofit2.Response
import java.io.*
import java.text.SimpleDateFormat
import java.util.*

class MainActivity: AppCompatActivity() {

    private var img: ImageView? = null
    private var predicted: TextView? = null
    private var confidence: TextView? = null
    private var waiting: TextView? = null
    private var download: FloatingActionButton? = null

    private var model:Classifier? = null
    private var interpreter:Interpreter? = null
    private var toast: Toast? = null
    private var readyToShare = false
    private var imgResponse: String? = null
    private var realUriFromLoadedImage: String? = null

    private val dogConst = "Dog"
    private val catConst = "Cat"
    private val permission = Manifest.permission.WRITE_EXTERNAL_STORAGE
    private val permissionCodeWrite = 666
    private val loadImageGallery = 555
    private var loading = 0
    private var snackbar: Snackbar? = null

    companion object {
        val classes = listOf("Cat", "Dog")
    }

    /**
     * onCreate: Set of the views
     *      The imageView window is resized to match Width = Height
     *      Creation of the custom toolbar
     *      Load of the tflite model
     *      Performs the first request to load an image
     * */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val displayMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(displayMetrics)

        img = findViewById(R.id.image_to_classify)

        if (resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
            img?.layoutParams!!.height = displayMetrics.widthPixels - 1
        } else {
            img?.layoutParams!!.height = displayMetrics.heightPixels - 280
        }

        download = findViewById(R.id.download)
        predicted = findViewById(R.id.classified_as)
        confidence = findViewById(R.id.confidence_num)
        waiting = findViewById(R.id.waiting)

        val toolbar: Toolbar? = findViewById(R.id.supportBar)
        setSupportActionBar(toolbar)
        supportActionBar?.title = ""

        model = Classifier(this)
        interpreter = model?.loadModel()

        val cl = classes.random()
        setUpRequestAndPerform(cl)
    }

    /**
     * onRequestPermissionsResult: Checks if the 'permission' is already granted or changed
     * */
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>,
        grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == permissionCodeWrite) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                download(findViewById(android.R.id.content))
            }
        }
    }

    /**
     * onActivityResult: Retrieves the image from the gallery and populates the UI with it
     * */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK && requestCode == loadImageGallery) {
            try {
                val imageUri = data?.data
                val imageStream = contentResolver.openInputStream(imageUri!!)
                val selectedImage = BitmapFactory.decodeStream(imageStream)
                realUriFromLoadedImage = getRealPathFromURI(imageUri)
                img?.setImageBitmap(rotate(selectedImage, realUriFromLoadedImage!!))
                imgResponse = null
                classify()
            } catch (e: NullPointerException) {
                e.printStackTrace()
                makeText(getString(R.string.toast_image_err_gallery))
            } catch (e: IOException) {
                e.printStackTrace()
                makeText(getString(R.string.toast_image_err_gallery))
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    /**
     * onOptionsItemSelected: Manages the menu options
     *      share_image: Shares the image on social media
     *      load_image: Load image from device to be classified
     * */
    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId) {
            R.id.share_image -> {
                if (readyToShare) {
                    val intent = Intent(Intent.ACTION_SEND)
                    if (imgResponse != null) {
                        val msg = "Look at this cutie <3\n $imgResponse"
                        intent.putExtra(Intent.EXTRA_TEXT, msg)
                        intent.type = "text/*"
                    } else {
                        val msg = "Look at this cutie <3"
                        intent.putExtra(Intent.EXTRA_TEXT, msg)
                        intent.type = "text/*"
                        intent.putExtra(Intent.EXTRA_STREAM, Uri.parse(realUriFromLoadedImage))
                        intent.type = "image/*"
                    }
                    startActivity(intent)
                } else {
                    makeText(getString(R.string.toast_image_not_ready_to_share))
                }
            }
            R.id.load_image -> {
                val photoPickerIntent = Intent(Intent.ACTION_PICK)
                photoPickerIntent.type = "image/*"
                startActivityForResult(photoPickerIntent, loadImageGallery)
            }
        }
        return true
    }

    /**
     * buildUri: Builds the URI depending on the type of image, dog or cat.
     * @param cl: Type of image
     * @return String with the URI
     */
    private fun buildUri(cl: String): String {
        if (cl == dogConst) {
            val builder = Uri.Builder()
                .scheme("https")
                .authority("dog.ceo")
            return builder.build().toString()
        } else {
            val builder = Uri.Builder()
                .scheme("https")
                .authority("api.thecatapi.com")
            return builder.build().toString()
        }
    }

    /**
     * getImageFromResponse: From the API response, paints the image onto the ImageView
     * @param service: Instance of the interface providing the call to the API
     * @param cl: Type of image
     * */
    private fun getImageFromResponse(service: RetrofitInstance.GetService, cl: String) {
        if (cl == dogConst) {
            val call = service.getDogPhotos()
            call.enqueue(object: retrofit2.Callback<DogModel> {
                override fun onFailure(call: Call<DogModel>, t: Throwable) {
                    setError()
                }

                override fun onResponse(call: Call<DogModel>, response: Response<DogModel>) {
                    if (response.isSuccessful) {
                        snackbar?.dismiss()
                        val json = response.body()
                        imgResponse = json?.message
                        paintImage(imgResponse)
                    } else {
                        setError()
                    }
                }
            })
        } else {
            val call = service.getCatPhotos()
            call.enqueue(object: retrofit2.Callback<List<CatModel>> {
                override fun onFailure(call: Call<List<CatModel>>, t: Throwable) {
                    setError()
                }

                override fun onResponse(call: Call<List<CatModel>>, response: Response<List<CatModel>>) {
                    if (response.isSuccessful) {
                        snackbar?.dismiss()
                        val json = response.body()
                        imgResponse = json!![0].url
                        paintImage(imgResponse)
                    } else {
                        setError()
                    }
                }
            })
        }
    }

    /**
     * paintImage: Uses Picasso library to populate imageView with URL imgResponse
     * @param imgResponse: URL that points to image from API
     **/
    private fun paintImage(imgResponse: String?) {
        Picasso.get()
            .load(imgResponse)
            .config(Bitmap.Config.ARGB_8888)
            .into(img, object: Callback {
                override fun onSuccess() {
                    readyToShare = true
                    classify()
                }
                override fun onError(err: Exception?) {
                    readyToShare = false
                    waiting?.text = getString(R.string.failure_connection)
                }
            })
        loading = 0
    }

    /**
     * setUpRequestAndPerform: Builds the URI, call the API (Dog API or Cat API) and paints the image
     * @param cl: Type of image
     * */
    private fun setUpRequestAndPerform(cl: String) {
        waiting?.visibility = View.VISIBLE
        predicted?.visibility = View.GONE
        confidence?.visibility = View.GONE
        waiting?.text = getString(R.string.wait_for_img)
        download?.hide()

        val url = buildUri(cl)
        val service = RetrofitInstance(url).getRetrofitInstance().create(RetrofitInstance.GetService::class.java)
        getImageFromResponse(service, cl)
    }

    /**
     * classify: Only called when Picasso is done loading the image from the URL. Gets the image
     * from the ImageView as a bitmap and it is fed to the tflite model. The results of the predictions
     * are updated in UI.
     * */
    private fun classify() {
        val bitmap = (img?.drawable as BitmapDrawable).bitmap
        val results = model?.evaluate(interpreter, bitmap)
        waiting?.visibility = View.GONE
        predicted?.visibility = View.VISIBLE
        confidence?.visibility = View.VISIBLE
        download?.show()
        predicted?.text = results?.get(0)
        confidence?.text = results?.get(1)
    }

    /**
     * setError: Updates the UI when a Failure occurs on loading the image from API
     * */
    private fun setError() {
        waiting?.text = getString(R.string.failure_connection)
        img?.setImageBitmap(getDrawable(R.mipmap.crying)?.toBitmap())
        snackbar = Snackbar.make(findViewById(R.id.main_layout),
            getString(R.string.snackbar_error),
            Snackbar.LENGTH_INDEFINITE)
        snackbar?.setAction(getString(R.string.snackbar_action), View.OnClickListener {
            val r = Random()
            if (r.nextInt(2) == 0) fetchCat(null)
            else fetchDog(null)
        })
        snackbar?.setActionTextColor(ContextCompat.getColor(this, R.color.retry))
        snackbar?.show()
        loading = 0
    }

    /**
     * saveImageToStorage: Saves the ImageView image to the external Storage
     * @param bitmap: Bitmap representing the image
     */
    private fun saveImageToStorage(bitmap: Bitmap) {
        val state = Environment.getExternalStorageState()
        if (Environment.MEDIA_MOUNTED == state) {
            val dir = File(
                Environment.getExternalStorageDirectory().absolutePath,
                getString(R.string.app_name)
            )
            val isDirCreated = dir.mkdir()
            if (dir.exists() || isDirCreated) {
                try {
                    val timeStamp = SimpleDateFormat(getString(R.string.file_format_name), Locale.ENGLISH).format(Date())
                    val fname = "CVD_$timeStamp.jpg"
                    val file = File(dir, fname)

                    val out = FileOutputStream(file)
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
                    out.flush()
                    out.close()
                    makeText(getString(R.string.toast_image_saved))
                } catch (err: IOException) {
                    makeText(getString(R.string.toast_error_saving))
                }

            } else {
                makeText(getString(R.string.toast_error_directory))
            }
        } else {
            makeText(getString(R.string.toast_no_sd))
        }
    }

    /**
     * checkPermits: Checks if the 'permission' has been granted (WRITE_EXTERNAL_STORAGE)
     * */
    private fun checkPermits(): Boolean {
        return ActivityCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * rotate: Applies rotation on the image from the gallery based in the Efix information
     * @param bitmap: Bitmap of the loaded image
     * @param path: Path to selected image
     * @return: New bitmap with the rotated image
     * */
    private fun rotate(bitmap: Bitmap, path: String): Bitmap {
        val info = ExifInterface(path)
        val orientation = info.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        val matrix = Matrix()

        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
        }

        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    /**
     * getRealPathFromURI: Gets the real URI path to the image fetched from gallery
     * @param contentURI: Uri retrieved from the intent
     * @return real URI
     * */
    private fun getRealPathFromURI(contentURI: Uri): String? {
        var res: String? = null
        val projection = arrayOf(MediaStore.Images.Media.DATA)
        val cursor = contentResolver.query(contentURI, projection, null, null, null) ?: return null
        if (cursor.moveToFirst()) {
            val columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
            res = cursor.getString(columnIndex)
        }
        cursor.close()
        return res
    }

    private fun makeText(msg: String) {
        toast?.cancel()
        toast = Toast.makeText(this, msg, Toast.LENGTH_SHORT)
        toast?.show()
    }

    /**
     * fetchDog: Calls setUpRequestAndPerform with type 'Dog'
     * */
    fun fetchDog(view: View?) {
        readyToShare = false
        if (loading == 0) {
            setUpRequestAndPerform(dogConst)
            loading++
        }
    }

    /**
     * fetchCat: Calls setUpRequestAndPerform with type 'Cat'
     * */
    fun fetchCat(view: View?) {
        readyToShare = false
        if (loading == 0) {
            setUpRequestAndPerform(catConst)
            loading++
        }
    }

    /**
     * download: Calls saveImageToStorage
     * */
    fun download(view: View?) {
        if (checkPermits()) {
            val bitmap = (img?.drawable as BitmapDrawable).bitmap
            saveImageToStorage(bitmap)
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(permission), permissionCodeWrite)
        }
    }
}
