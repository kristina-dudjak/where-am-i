package hr.ferit.kristinadudjak.whereami

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.location.Geocoder
import android.media.SoundPool
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.net.toUri
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import hr.ferit.kristinadudjak.whereami.databinding.ActivityMainBinding
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.util.*

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private val REQUEST_CODE = 100
    private lateinit var soundPool: SoundPool
    private var soundMap: HashMap<Int, Int> = HashMap()


    @SuppressLint("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 1)
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), 1)

        createNotificationChannels(this)

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)

        binding.bTakePhoto.setOnClickListener{
            val bitmap = getScreenShotFromView(binding.cardView)
            if (bitmap != null) {
                val fileUri = saveMediaToStorage(bitmap)
                displayNotification(fileUri)
            }
        }

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        this.loadSounds()
        fusedLocationProviderClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                val geocoder = Geocoder(this, Locale.getDefault())
                mapFragment.getMapAsync { googleMap ->
                    val latLng = LatLng(location.latitude, location.longitude)
                    val markerOptions = MarkerOptions().position(latLng).title("Im here")
                    googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 10F))
                    googleMap.addMarker(markerOptions)
                }
                try {
                    val addresses =
                        geocoder.getFromLocation(location.latitude, location.longitude, 1)
                    binding.tvLatitude.text = "Latitude :" + addresses[0].latitude
                    binding.tvLongitude.text = "Longitude :" + addresses[0].longitude
                    binding.tvAddress.text = "Address :" + addresses[0].getAddressLine(0)
                    binding.tvCity.text = "City :" + addresses[0].locality
                    binding.tvCountry.text = "Country :" + addresses[0].countryName

                } catch (e: Exception) {
                    e.printStackTrace()
                }
            } else {
                askPermission()
            }
        }

        val pinDescriptors = listOf(
            BitmapDescriptorFactory.HUE_AZURE,
            BitmapDescriptorFactory.HUE_BLUE,
            BitmapDescriptorFactory.HUE_CYAN,
            BitmapDescriptorFactory.HUE_GREEN,
            BitmapDescriptorFactory.HUE_MAGENTA,
            BitmapDescriptorFactory.HUE_ORANGE,
            BitmapDescriptorFactory.HUE_YELLOW,
            BitmapDescriptorFactory.HUE_ROSE,
            BitmapDescriptorFactory.HUE_VIOLET
        )
        mapFragment.getMapAsync { googleMap ->
            googleMap.setOnMapClickListener { latLng ->
                playSound(R.raw.click)
                val bitmapDescriptor = BitmapDescriptorFactory.defaultMarker(pinDescriptors.random())
                val markerOptions = MarkerOptions().position(latLng).title("New pin").icon(bitmapDescriptor)
                googleMap.addMarker(markerOptions)
            }
        }
    }

    private fun displayNotification(fileUri: Uri?) {
        var pendingIntent: PendingIntent? = null
        if (fileUri != null) {
            val intent = Intent(Intent.ACTION_VIEW,fileUri)
            pendingIntent = PendingIntent.getActivity(
                this,
                0,
                intent,
                PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Where am I")
            .setContentText("Spremljena je nova slika")
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()
        NotificationManagerCompat.from(this)
            .notify(1001, notification)

    }

    private fun saveMediaToStorage(bitmap: Bitmap): Uri? {
        val fileName = "${System.currentTimeMillis()}.jpg"
        var fos: OutputStream? = null
        var fileUri: Uri? = null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            this.contentResolver?.also { resolver ->

                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/jpg")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
                }
                val imageUri: Uri? = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                fos = imageUri?.let { resolver.openOutputStream(it) }
                fileUri = imageUri
            }
        } else {
            val imagesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            val image = File(imagesDir, fileName)
            fos = FileOutputStream(image)
            fileUri = image.toUri()
        }
        fos?.use {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, it)
            Toast.makeText(this , "Saved to Gallery" , Toast.LENGTH_SHORT).show()
        }
        return fileUri
    }

    private fun getScreenShotFromView(v: View): Bitmap? {
        var screenshot: Bitmap? = null
        try {
            screenshot = Bitmap.createBitmap(v.measuredWidth, v.measuredHeight, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(screenshot)
            v.draw(canvas)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return screenshot
    }

    private fun loadSounds() {
        this.soundPool = SoundPool.Builder().setMaxStreams(10).build()
        this.soundMap[R.raw.click] = this.soundPool.load(this, R.raw.click, 1)
    }

    private fun playSound(selectedSound: Int) {
        val soundID = this.soundMap[selectedSound] ?: 0
        this.soundPool.play(soundID, 1f, 1f, 1, 0, 1f)
    }


    private fun askPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), REQUEST_CODE
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                super.onRequestPermissionsResult(requestCode, permissions, grantResults)
            } else {
                Toast.makeText(this, "Permission not granted", Toast.LENGTH_SHORT).show()
            }
        }
    }
}