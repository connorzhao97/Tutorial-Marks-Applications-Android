package au.edu.utas.yucongz.assignment2

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.ImageView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import au.edu.utas.yucongz.assignment2.databinding.ActivityAddStudentBinding
import com.google.firebase.firestore.ktx.toObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class AddStudentActivity : AppCompatActivity() {

    private lateinit var ui: ActivityAddStudentBinding
    private var currentPhotoPath: String? = ""
    private var gotPhoto: Boolean = false

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ui = ActivityAddStudentBinding.inflate(layoutInflater)
        setContentView(ui.root)

        //https://developer.android.com/reference/kotlin/android/app/ActionBar#setdisplayhomeasupenabled
        //https://developer.android.com/reference/kotlin/android/app/ActionBar#setdisplayshowhomeenabled
        setSupportActionBar(ui.toolbarAddStudent)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        ui.btnAddAvatar.setOnClickListener {
            requestToTakeAPicture()
        }

        ui.btnAddStudent.setOnClickListener {
            if (ui.editTextAddID.text.trim().isNullOrBlank() || ui.editTextAddName.text.trim().isNullOrBlank()) {
                //https://developer.android.com/reference/kotlin/android/app/AlertDialog.Builder
                var builder = AlertDialog.Builder(this)
                builder.setTitle("Alert")
                builder.setMessage("Student name or id cannot be empty. Please Check!")
                builder.setPositiveButton("OK", null)
                builder.create()
                builder.show()
            } else {
                ui.btnAddStudent.isEnabled = false
                ui.btnAddStudent.text = "ADDING"

                var id: String? = null

                if (!gotPhoto) {
                    currentPhotoPath = ""
                }

                val newStudent = Student(
                    id = id,
                    studentID = ui.editTextAddID.text.trim().toString().toInt(),
                    name = ui.editTextAddName.text.trim().toString(),
                    avatarUrl = currentPhotoPath,
                    scores = Score()
                )

                studentsCollection
                    .add(newStudent) //Add the new student
                    .addOnSuccessListener { it ->
                        id = it.id
                        newStudent.id = id
                        studentsCollection.document("$id")
                            .update(
                                "id",
                                id
                            ) // Update the new student id after adding the student
                            .addOnSuccessListener {
                                studentsCollection.orderBy("studentID")
                                    .get() //Get the new student list after updating the student
                                    .addOnSuccessListener { result ->
                                        students.clear()
                                        for (document in result) {
                                            val student = document.toObject<Student>()
                                            Log.d("Student", student.toString())
                                            students.add(student)
                                        }
                                        tempStudents.clear()
                                        tempStudents.addAll(students)
                                        Toast.makeText(
                                            this,
                                            "Student: ${ui.editTextAddName.text.trim()} ${ui.editTextAddID.text.trim()} added successfully!",
                                            Toast.LENGTH_LONG
                                        ).show()
                                        finish()
                                    }
                            }
                            .addOnFailureListener {
                                Log.e(FIREBASE_TAG, it.toString())
                            }
                    }
                    .addOnFailureListener {
                        Log.e(FIREBASE_TAG, it.toString())
                        ui.btnAddStudent.isEnabled = true
                        ui.btnAddStudent.text = "ADD THE STUDENT"
                    }
            }
        }
    }


    /****  Camera Part ****/
    @RequiresApi(Build.VERSION_CODES.M)
    private fun requestToTakeAPicture() {
        requestPermissions(
            arrayOf(Manifest.permission.CAMERA),
            REQUEST_IMAGE_CAPTURE
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            REQUEST_IMAGE_CAPTURE -> {
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    // Permission is granted.
                    takeAPicture()
                } else {
                    Toast.makeText(
                        this,
                        "Cannot access camera, permission denied",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun takeAPicture() {
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)

        try {
            val photoFile: File = createImageFile()!!
            val photoURI: Uri = FileProvider.getUriForFile(
                this,
                "au.edu.utas.yucongz.assignment2",
                photoFile
            )
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
        } catch (e: Exception) {
            Log.d("TakeAPicture", e.toString())
        }
    }

    private fun createImageFile(): File {
        // Create an image file name
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val storageDir: File = this.getExternalFilesDir(Environment.DIRECTORY_PICTURES)!!
        return File.createTempFile(
            "JPEG_${timeStamp}_", /* prefix */
            ".jpg", /* suffix */
            storageDir /* directory */
        ).apply {
            // Save a file: path for use with ACTION_VIEW intents
            currentPhotoPath = absolutePath
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == AppCompatActivity.RESULT_OK) {
            setPic(ui.avatarAddStudent)
            gotPhoto = true
        }
    }

    private fun setPic(imageView: ImageView) {
        // Get the dimensions of the View
        val targetW: Int = imageView.measuredWidth
        val targetH: Int = imageView.measuredHeight


        val bmOptions = BitmapFactory.Options().apply {
            // Get the dimensions of the bitmap
            inJustDecodeBounds = true

            BitmapFactory.decodeFile(currentPhotoPath, this)

            val photoW: Int = outWidth
            val photoH: Int = outHeight

            // Determine how much to scale down the image
            val scaleFactor: Int = Math.max(1, Math.min(photoW / targetW, photoH / targetH))

            // Decode the image file into a Bitmap sized to fill the View
            inJustDecodeBounds = false
            inSampleSize = scaleFactor
        }
        BitmapFactory.decodeFile(currentPhotoPath, bmOptions)?.also { bitmap ->
            imageView.setImageBitmap(bitmap)
        }
    }
}
