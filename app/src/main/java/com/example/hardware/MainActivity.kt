package com.example.hardware

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanIntentResult
import com.journeyapps.barcodescanner.ScanOptions
import android.util.Log

class MainActivity : AppCompatActivity() {
    private lateinit var database: DatabaseReference
    private lateinit var resultTextView: TextView
    private lateinit var scanQrButton: Button
    private lateinit var qrCounterTextView: TextView
    private lateinit var logoutButton: Button
    private var qrCounter: Int = 0
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().reference.child("claves")
        resultTextView = findViewById(R.id.resultTextView)
        scanQrButton = findViewById(R.id.scanQrButton)
        qrCounterTextView = findViewById(R.id.qrCounterTextView)
        logoutButton = findViewById(R.id.logoutButton)

        val barcodeLauncher = registerForActivityResult(ScanContract()) { result: ScanIntentResult ->
            if (result.contents == null) {
                Toast.makeText(this, "Cancelled", Toast.LENGTH_LONG).show()
            } else {
                val qrContent = result.contents
                resultTextView.text = qrContent
                resultTextView.visibility = TextView.VISIBLE
                qrCounter++
                qrCounterTextView.text = "QR Codes Scanned: $qrCounter"
                checkQRCodeStatus(qrContent)
            }
        }

        val options = ScanOptions().apply {
            setDesiredBarcodeFormats(ScanOptions.QR_CODE)
            setPrompt("Leer código QR")
            setCameraId(0)
            setBeepEnabled(false)
            setBarcodeImageEnabled(true)
        }

        scanQrButton.setOnClickListener {
            barcodeLauncher.launch(options)
        }

        logoutButton.setOnClickListener {
            auth.signOut()
            val intent = Intent(this, Login::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }
    }

    private fun checkQRCodeStatus(qrContent: String) {
        Log.d("MainActivity", "Iniciando checkQRCodeStatus con QR content: $qrContent")
        database.get().addOnSuccessListener { snapshot ->
            if (snapshot.exists()) {
                Log.d("MainActivity", "Datos obtenidos del snapshot: ${snapshot.value}")
                var qrFound = false
                for (childSnapshot in snapshot.children) {
                    val qrKey = childSnapshot.key
                    val status = childSnapshot.child("status").getValue(String::class.java)
                    Log.d("MainActivity", "QR Key: $qrKey, Estado: $status")
                    if (qrKey == qrContent) {
                        qrFound = true
                        when (status) {
                            "qr generado" -> {
                                childSnapshot.child("status").ref.setValue("qr utilizado")
                                Toast.makeText(this, "Buen viaje", Toast.LENGTH_LONG).show()
                            }
                            "qr utilizado" -> {
                                Toast.makeText(this, "QR utilizado, Genere un nuevo QR para ingresar", Toast.LENGTH_LONG).show()
                            }
                            else -> {
                                Toast.makeText(this, "Estado desconocido", Toast.LENGTH_LONG).show()
                            }
                        }
                        break
                    }
                }
                if (!qrFound) {
                    Toast.makeText(this, "Código QR no encontrado", Toast.LENGTH_LONG).show()
                }
            } else {
                Log.d("MainActivity", "Snapshot no existe o está vacío")
                Toast.makeText(this, "Código QR no encontrado", Toast.LENGTH_LONG).show()
            }
        }.addOnFailureListener {
            Toast.makeText(this, "Error al leer el QR: ${it.message}", Toast.LENGTH_LONG).show()
            Log.e("MainActivity", "Error al leer el QR: ${it.message}")
        }
    }
}
