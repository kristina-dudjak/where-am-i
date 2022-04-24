package hr.ferit.kristinadudjak.whereami
import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Geocoder
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import hr.ferit.kristinadudjak.whereami.databinding.ActivityMainBinding
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private val REQUEST_CODE = 100


    @SuppressLint("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment

        fusedLocationProviderClient.lastLocation.addOnSuccessListener { location ->
            if (location != null){
                val geocoder = Geocoder( this, Locale.getDefault())
                mapFragment.getMapAsync { googleMap ->
                    val latLng = LatLng(location.latitude, location.longitude)
                    val markerOptions = MarkerOptions().position(latLng).title("Im here")
                    googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15F))
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

                } catch (e: Exception){
                    e.printStackTrace()
                }
            } else {
                askPermission()
            }
        }
    }

    private fun askPermission() {
        ActivityCompat.requestPermissions(this,
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), REQUEST_CODE)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode==REQUEST_CODE){
            if(grantResults.isNotEmpty() &&  grantResults[0] == PackageManager.PERMISSION_GRANTED){
                super.onRequestPermissionsResult(requestCode, permissions, grantResults)
            }  else {
                Toast.makeText(this, "Permission not granted", Toast.LENGTH_SHORT).show()
            }
        }
    }
}