package au.edu.utas.yucongz.assignment2

import android.Manifest
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import au.edu.utas.yucongz.assignment2.databinding.ActivityStudentDetailsBinding
import au.edu.utas.yucongz.assignment2.databinding.IndividualSummaryItemBinding
import com.google.firebase.firestore.ktx.toObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

const val REQUEST_IMAGE_CAPTURE = 1

class StudentDetailsActivity : AppCompatActivity(), (DialogInterface, Int) -> Unit {

    private lateinit var ui: ActivityStudentDetailsBinding
    private lateinit var studentObject: Student
    private var currentPhotoPath: String? = ""
    private var editedPhoto: Boolean = false // check whether student photo edited
    private var position: Int = -1
    private var summaryScore = 0.0

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ui = ActivityStudentDetailsBinding.inflate(layoutInflater)
        setContentView(ui.root)

        //https://developer.android.com/reference/kotlin/android/app/ActionBar#setdisplayhomeasupenabled
        //https://developer.android.com/reference/kotlin/android/app/ActionBar#setdisplayshowhomeenabled
        setSupportActionBar(ui.toolbarStudentDetails)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        //get student object from intent
        position = intent.getIntExtra(STUDENT_DETAILS_INDEX, -1)
        studentObject = tempStudents[position]

        //Holder and Adapter
        ui.individualScoresList.adapter = ScoreAdapter(scores = studentObject.scores)
        ui.individualScoresList.layoutManager = LinearLayoutManager(this)

        //Initialization layout
        ui.editTextDetailName.setText(studentObject.name)
        ui.editTextDetailID.setText(studentObject.studentID.toString())
        if (studentObject.avatarUrl != "") {
            //https://stackoverflow.com/questions/3004713/get-content-uri-from-file-path-in-android
            ui.avatarStudentDetail.setImageURI(Uri.fromFile(File(studentObject.avatarUrl)))
        }

        for (i in 0..11) {
            summaryScore += studentObject.scores!![i]
        }
        ui.txtDetailSummaryScore.text =
            "$summaryScore/1200 (${String.format("%.2f", summaryScore / 12.0)}%)"

        ui.btnUpdateAvatar.setOnClickListener {
            requestToTakeAPicture()
        }

    }

    /****  ActionBar (Menu) Part ****/
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_student_details, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here.
        return when (item.itemId) {
            R.id.app_bar_save -> return saveStudentDetailChanges()
            R.id.app_bar_delete -> return deleteStudent()
            R.id.app_bar_individual_share -> return shareIndividualScores()
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun saveStudentDetailChanges(): Boolean {
        if (ui.editTextDetailName.text.trim().isNullOrBlank() || ui.editTextDetailID.text.trim().isNullOrBlank()) {
            var builder = AlertDialog.Builder(this)
            builder.setTitle("Alert")
            builder.setMessage("Student name or id cannot be empty. Please Check!")
            builder.setPositiveButton("OK", null)
            builder.create()
            builder.show()
        } else {
            studentObject.name = ui.editTextDetailName.text.trim().toString()
            studentObject.studentID = ui.editTextDetailID.text.trim().toString().toInt()
            if (editedPhoto) {
                studentObject.avatarUrl = currentPhotoPath
            }
            studentsCollection.document("${studentObject.id}")
                .set(studentObject)
                .addOnSuccessListener {
                    Toast.makeText(
                        this,
                        "Student: ${studentObject.name} ${studentObject.studentID} updated successfully",
                        Toast.LENGTH_LONG
                    ).show()
                    finish()
                }
        }

        return true
    }

    private fun deleteStudent(): Boolean {
        //https://developer.android.com/reference/kotlin/android/app/AlertDialog.Builder
        //https://developer.android.com/reference/kotlin/android/content/DialogInterface.OnClickListener.html
        var builder = AlertDialog.Builder(this)
        builder.setTitle("Alert")
        builder.setMessage("Are you sure to delete student : ${studentObject.name} ${studentObject.studentID} ?")
        builder.setPositiveButton("Delete!", DialogInterface.OnClickListener { _, _ ->
            studentsCollection.document("${studentObject.id}")
                .delete() //Delete the student
                .addOnSuccessListener {
                    studentsCollection.orderBy("studentID")
                        .get()
                        .addOnSuccessListener { result ->
                            students.clear() //Get new student list
                            for (document in result) {
                                val student = document.toObject<Student>()
                                students.add(student)
                            }
                            tempStudents.clear()
                            tempStudents.addAll(students)
                            Toast.makeText(
                                this,
                                "Student ${studentObject.name} ${studentObject.studentID} deleted successfully!",
                                Toast.LENGTH_LONG
                            ).show()
                            finish()
                        }
                }
        })

        builder.setNegativeButton("Cancel", null)
        builder.create()
        builder.show()

        return true
    }

    private fun shareIndividualScores(): Boolean {
        var shareContent = """
                Student Name: ${studentObject.name},
                Student ID: ${studentObject.studentID},
                Week1 Score: ${studentObject.scores!!["week1"]},
                Week2 Score: ${studentObject.scores!!["week2"]},
                Week3 Score: ${studentObject.scores!!["week3"]},
                Week4 Score: ${studentObject.scores!!["week4"]},
                Week5 Score: ${studentObject.scores!!["week5"]},
                Week6 Score: ${studentObject.scores!!["week6"]},
                Week7 Score: ${studentObject.scores!!["week7"]},
                Week8 Score: ${studentObject.scores!!["week8"]},
                Week9 Score: ${studentObject.scores!!["week9"]},
                Week10 Score: ${studentObject.scores!!["week10"]},
                Week11 Score: ${studentObject.scores!!["week11"]},
                Week12 Score: ${studentObject.scores!!["week12"]},
                Summary Score: $summaryScore/1200 (${String.format("%.2f", summaryScore / 12.0)}%).
            """.trimIndent()
        var sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, shareContent)
            type = "text/plain"
        }
        startActivity(Intent.createChooser(sendIntent, "Share via..."))
        return true
    }

    override fun invoke(p1: DialogInterface, p2: Int) {}

    /****  RecyclerView Part ****/
    inner class ScoreHolder(var ui: IndividualSummaryItemBinding) : RecyclerView.ViewHolder(ui.root) {}

    inner class ScoreAdapter(private val scores: Score?) : RecyclerView.Adapter<ScoreHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ScoreHolder {
            val ui = IndividualSummaryItemBinding.inflate(layoutInflater, parent, false)
            return ScoreHolder(ui)
        }

        override fun onBindViewHolder(holder: ScoreHolder, position: Int) {
            holder.ui.txtIndividualWeek.text = studentObject.scores!!.getWeek(position)
            holder.ui.txtIndividualScore.text = String.format("%.2f", studentObject.scores!![position])
        }

        override fun getItemCount(): Int {
            return 12
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
//                    Toast.makeText(context, "permission Granted", Toast.LENGTH_LONG).show()
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
            setPic(ui.avatarStudentDetail)
            editedPhoto = true
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
