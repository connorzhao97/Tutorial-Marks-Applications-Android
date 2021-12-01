package au.edu.utas.yucongz.assignment2

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.AdapterView
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import au.edu.utas.yucongz.assignment2.databinding.ActivityMainBinding
import au.edu.utas.yucongz.assignment2.databinding.StudentMarkingItemBinding
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.ktx.Firebase
import java.io.File

const val FIREBASE_TAG = "Firebase:"
val db = Firebase.firestore
val studentsCollection = db.collection("students")
val markingSchemeCollection = db.collection("scheme")

var students = mutableListOf<Student>()// The mutable list of original students data from database
var tempStudents = mutableListOf<Student>() // The temporary list of students for searching or sorting
var markingSchemes = MarkingScheme() //Marking schemes of each week from database

class MainActivity : AppCompatActivity() {

    private var selectedWeek: String? = null
    private var selectedMarkingScheme: String? = null
    private lateinit var ui: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ui = ActivityMainBinding.inflate(layoutInflater)
        setContentView(ui.root)
        setSupportActionBar(ui.toolbarMain)

        ui.txtNumber.text = "Loading..."

        //Holder and Adapter
        ui.markingList.adapter = StudentAdapter(students = students)
        ui.markingList.layoutManager = LinearLayoutManager(this)

        //Spinner for selecting week
        ui.spinnerWeeks.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
                selectedWeek = p0?.getItemAtPosition(p2).toString()

                when (markingSchemes[selectedWeek!!]) {
                    "Attendance" -> ui.spinnerMarkingScheme.setSelection(0)
                    "Multiple Checkpoints" -> ui.spinnerMarkingScheme.setSelection(1)
                    "Score out of X" -> ui.spinnerMarkingScheme.setSelection(2)
                    "Grade Level (HD)" -> ui.spinnerMarkingScheme.setSelection(3)
                    "Grade Level (A)" -> ui.spinnerMarkingScheme.setSelection(4)
                    else -> ui.spinnerMarkingScheme.setSelection(0)
                }
                (ui.markingList.adapter as StudentAdapter).notifyDataSetChanged()
            }

            override fun onNothingSelected(p0: AdapterView<*>?) {}
        }

        //Spinner for selecting marking scheme
        ui.spinnerMarkingScheme.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
                    selectedMarkingScheme = p0?.getItemAtPosition(p2).toString()
                    var previousMarkingScheme = markingSchemes[selectedWeek!!]

                    if (selectedMarkingScheme != markingSchemes[selectedWeek!!]) {
                        //https://developer.android.com/reference/kotlin/android/app/AlertDialog.Builder
                        //https://developer.android.com/reference/kotlin/android/content/DialogInterface.OnClickListener.html
                        var builder = AlertDialog.Builder(p0!!.context)
                        builder.setTitle("Alert")
                        builder.setMessage("If You Change The Marking Scheme For This Week, All Previous Marks Will Be Clear!")
                        builder.setPositiveButton(
                            "Change!",
                            DialogInterface.OnClickListener { _, _ ->
                                markingSchemeCollection.document("${markingSchemes.id}")
                                    .update("$selectedWeek", "$selectedMarkingScheme")
                                    .addOnSuccessListener {
                                        markingSchemes[selectedWeek!!] = selectedMarkingScheme!!
                                        //clear all students' score
                                        //https://firebase.google.com/docs/firestore/manage-data/transactions#batched-writes
                                        db.runBatch { batch ->
                                            for (student in students) {
                                                batch.update(
                                                    studentsCollection.document("${student.id}"),
                                                    "scores.$selectedWeek",
                                                    0
                                                )
                                                student.scores!!["$selectedWeek"] = 0.00
                                            }
                                        }.addOnCompleteListener {
                                            (ui.markingList.adapter as StudentAdapter).notifyDataSetChanged()
                                        }
                                    }
                            })
                        builder.setNegativeButton(
                            "Cancel",
                            DialogInterface.OnClickListener { _, _ ->
                                when (previousMarkingScheme) {
                                    "Attendance" -> ui.spinnerMarkingScheme.setSelection(0)
                                    "Multiple Checkpoints" -> ui.spinnerMarkingScheme.setSelection(1)
                                    "Score out of X" -> ui.spinnerMarkingScheme.setSelection(2)
                                    "Grade Level (HD)" -> ui.spinnerMarkingScheme.setSelection(3)
                                    "Grade Level (A)" -> ui.spinnerMarkingScheme.setSelection(4)
                                }
                            })
                        builder.create().show()
                    } else {
                        (ui.markingList.adapter as StudentAdapter).notifyDataSetChanged()
                    }
                }

                override fun onNothingSelected(p0: AdapterView<*>?) {}
            }

        //Check marking scheme exists or not. If not exist, create a new one
        markingSchemeCollection
            .get()
            .addOnSuccessListener { result ->
                if (result.size() == 0) {
                    val markingScheme = MarkingScheme()
                    markingSchemeCollection
                        .add(markingScheme)
                } else {
                    markingSchemes = result.documents[0].toObject<MarkingScheme>()!!
                    markingSchemes.id = result.documents[0].id
                }
            }
        //Getting all student data and add to students mutable list
        studentsCollection.orderBy("studentID")
            .get()
            .addOnSuccessListener { result ->
                students.clear()
                for (document in result) {
                    val student = document.toObject<Student>()
                    students.add(student)
                    (ui.markingList.adapter as StudentAdapter).notifyDataSetChanged()
                }
                ui.txtNumber.text = "${students.size} Students"
            }
    }

    override fun onResume() {
        super.onResume()
        ui.txtNumber.text = "${students.size} Students"
        (ui.markingList.adapter as StudentAdapter).notifyDataSetChanged()
    }


    /****  RecyclerView Part ****/
    inner class StudentHolder(var ui: StudentMarkingItemBinding) : RecyclerView.ViewHolder(ui.root)

    inner class StudentAdapter(private val students: MutableList<Student>) :
        RecyclerView.Adapter<StudentHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StudentHolder {
            val ui = StudentMarkingItemBinding.inflate(layoutInflater, parent, false)
            return StudentHolder(ui)
        }

        override fun getItemCount(): Int {
            return students.size
        }

        override fun onBindViewHolder(holder: StudentHolder, position: Int) {
            val student = students[position]

            holder.ui.txtMarkingItemName.text = student.name
            holder.ui.txtMarkingItemID.text = student.studentID.toString()
            if (student.avatarUrl != "") {
                //https://stackoverflow.com/questions/3004713/get-content-uri-from-file-path-in-android
                holder.ui.avatarMarkingItem.setImageURI(Uri.fromFile(File(student.avatarUrl)))
            } else {
                holder.ui.avatarMarkingItem.setImageResource(R.drawable.ic_user)
            }

            //Mark students with different marking scheme
            when (selectedMarkingScheme) {
                //Mark students with attendance
                "Attendance" -> {
                    holder.ui.AttendanceLayout.visibility = View.VISIBLE
                    holder.ui.CheckpointLinearLayout.visibility = View.GONE
                    holder.ui.ScoreOutOfLinearLayout.visibility = View.GONE
                    holder.ui.GradeHDLinearLayout.visibility = View.GONE
                    holder.ui.GradeALinearLayout.visibility = View.GONE

                    //Initialization
                    holder.ui.checkBoxAttendance.isChecked =
                        student.scores!!["$selectedWeek"] == 100.0

                    //https://developer.android.com/reference/kotlin/android/widget/CompoundButton.OnCheckedChangeListener
                    //Update Score
                    holder.ui.checkBoxAttendance.setOnCheckedChangeListener { checkBox, isChecked ->
                        /** IMPORTANT: Check the checkBox is initialized or pressed **/
                        if (checkBox.isPressed) {
                            var score = 0.0
                            if (isChecked) {
                                score = 100.0
                            }
                            studentsCollection.document("${student.id}")
                                .update(mapOf("scores.$selectedWeek" to score))
                                .addOnSuccessListener {
                                    student.scores!!["$selectedWeek"] = score
                                    Toast.makeText(
                                        holder.ui.root.context,
                                        "${student.name} score updated successfully.",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                        }
                    }
                }
                //Mark students with multiple checkpoints
                "Multiple Checkpoints" -> {
                    holder.ui.AttendanceLayout.visibility = View.GONE
                    holder.ui.CheckpointLinearLayout.visibility = View.VISIBLE
                    holder.ui.ScoreOutOfLinearLayout.visibility = View.GONE
                    holder.ui.GradeHDLinearLayout.visibility = View.GONE
                    holder.ui.GradeALinearLayout.visibility = View.GONE

                    when (student.scores!!["$selectedWeek"]) {
                        100.0 -> {
                            holder.ui.checkpoint1.isChecked = true
                            holder.ui.checkpoint2.isEnabled = true
                            holder.ui.checkpoint2.isChecked = true
                        }
                        50.0 -> {
                            holder.ui.checkpoint1.isChecked = true
                            holder.ui.checkpoint2.isEnabled = true
                            holder.ui.checkpoint2.isChecked = false
                        }
                        0.0 -> {
                            holder.ui.checkpoint1.isChecked = false
                            holder.ui.checkpoint2.isEnabled = false
                            holder.ui.checkpoint2.isChecked = false
                        }
                    }

                    holder.ui.checkpoint1.setOnCheckedChangeListener { checkBox1, isChecked ->
                        if (checkBox1.isPressed) {
                            var score = 0.0
                            if (isChecked) {
                                score = 50.0
                            }
                            studentsCollection.document("${student.id}")
                                .update(mapOf("scores.$selectedWeek" to score))
                                .addOnSuccessListener {
                                    student.scores!!["$selectedWeek"] = score
                                    if (isChecked) {
                                        holder.ui.checkpoint2.isChecked = false
                                        holder.ui.checkpoint2.isEnabled = true
                                    } else {
                                        holder.ui.checkpoint2.isChecked = false
                                        holder.ui.checkpoint2.isEnabled = false
                                    }

                                    Toast.makeText(
                                        holder.ui.root.context,
                                        "${student.name} score updated successfully.",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                        }
                    }

                    holder.ui.checkpoint2.setOnCheckedChangeListener { checkBox2, isChecked ->
                        if (checkBox2.isPressed) {
                            var score = 50.0
                            if (isChecked) {
                                score = 100.0
                            }
                            studentsCollection.document("${student.id}")
                                .update(mapOf("scores.$selectedWeek" to score))
                                .addOnSuccessListener {
                                    student.scores!!["$selectedWeek"] = score
                                    Toast.makeText(
                                        holder.ui.root.context,
                                        "${student.name} score updated successfully.",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                        }
                    }
                }
                //Mark students with score out of x
                "Score out of X" -> {
                    holder.ui.AttendanceLayout.visibility = View.GONE
                    holder.ui.CheckpointLinearLayout.visibility = View.GONE
                    holder.ui.ScoreOutOfLinearLayout.visibility = View.VISIBLE
                    holder.ui.GradeHDLinearLayout.visibility = View.GONE
                    holder.ui.GradeALinearLayout.visibility = View.GONE

                    holder.ui.txtMarkingScore.text = student.scores!!["$selectedWeek"].toString()

                    holder.ui.btnUpdateScore.setOnClickListener {
                        holder.ui.btnUpdateScore.visibility = View.GONE
                        holder.ui.txtMarkingScore.visibility = View.GONE
                        holder.ui.btnSaveScore.visibility = View.VISIBLE
                        holder.ui.editTextMarkingScore.visibility = View.VISIBLE
                        holder.ui.editTextMarkingScore.setText(student.scores!!["$selectedWeek"].toString())
                    }

                    holder.ui.btnSaveScore.setOnClickListener {
                        //https://developer.android.com/reference/kotlin/android/view/inputmethod/InputMethodManager#hidesoftinputfromwindow
                        //https://stackoverflow.com/questions/41790357/close-hide-the-android-soft-keyboard-with-kotlin
                        /** Hide keyboard **/
                        val imm: InputMethodManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                        imm.hideSoftInputFromWindow(holder.ui.root.windowToken, 0)

                        holder.ui.btnUpdateScore.visibility = View.VISIBLE
                        holder.ui.txtMarkingScore.visibility = View.VISIBLE
                        holder.ui.btnSaveScore.visibility = View.GONE
                        holder.ui.editTextMarkingScore.visibility = View.GONE

                        val score = holder.ui.editTextMarkingScore.text.trim().toString().toDouble()
                        if (score == student.scores!!["$selectedWeek"]) {
                            Toast.makeText(
                                holder.ui.root.context,
                                "Score does not change.",
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            if (score in 0.0..100.0) {
                                holder.ui.txtMarkingScore.text = holder.ui.editTextMarkingScore.text
                                studentsCollection.document("${student.id}")
                                    .update(mapOf("scores.$selectedWeek" to score))
                                    .addOnSuccessListener {
                                        student.scores!!["$selectedWeek"] = score
                                        Toast.makeText(
                                            holder.ui.root.context,
                                            "${student.name} score updated successfully.",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                            } else {
                                Toast.makeText(
                                    holder.ui.root.context,
                                    "Score out of range, input again!",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }
                }
                //Mark students with Grade Level (HD)
                "Grade Level (HD)" -> {
                    holder.ui.AttendanceLayout.visibility = View.GONE
                    holder.ui.CheckpointLinearLayout.visibility = View.GONE
                    holder.ui.ScoreOutOfLinearLayout.visibility = View.GONE
                    holder.ui.GradeHDLinearLayout.visibility = View.VISIBLE
                    holder.ui.GradeALinearLayout.visibility = View.GONE

                    //Initialization
                    when (student.scores!!["$selectedWeek"]) {
                        100.0 -> holder.ui.btnHDP.isChecked = true
                        80.0 -> holder.ui.btnHD.isChecked = true
                        70.0 -> holder.ui.btnDN.isChecked = true
                        60.0 -> holder.ui.btnCR.isChecked = true
                        50.0 -> holder.ui.btnPP.isChecked = true
                        0.0 -> holder.ui.btnNN.isChecked = true
                    }
                    //https://developer.android.com/reference/kotlin/android/widget/RadioGroup#setoncheckedchangelistener
                    //https://developer.android.com/reference/kotlin/android/widget/RadioGroup.OnCheckedChangeListener
                    //Update Score
                    holder.ui.btnGroupHD.setOnCheckedChangeListener(RadioGroup.OnCheckedChangeListener { _, checkedID ->
                        if (holder.ui.btnGroupHD.findViewById<RadioButton>(checkedID).isPressed) {
                            var score = 0.0
                            when (checkedID) {
                                R.id.btnHDP -> score = 100.0
                                R.id.btnHD -> score = 80.0
                                R.id.btnDN -> score = 70.0
                                R.id.btnCR -> score = 60.0
                                R.id.btnPP -> score = 50.0
                                R.id.btnNN -> score = 0.0
                            }
                            studentsCollection.document("${student.id}")
                                .update(mapOf("scores.$selectedWeek" to score))
                                .addOnSuccessListener {
                                    student.scores!!["$selectedWeek"] = score
                                    Toast.makeText(
                                        holder.ui.root.context,
                                        "${student.name} score updated successfully.",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                        }
                    })
                }
                //Mark students with Grade Level (A)
                "Grade Level (A)" -> {
                    holder.ui.AttendanceLayout.visibility = View.GONE
                    holder.ui.CheckpointLinearLayout.visibility = View.GONE
                    holder.ui.ScoreOutOfLinearLayout.visibility = View.GONE
                    holder.ui.GradeHDLinearLayout.visibility = View.GONE
                    holder.ui.GradeALinearLayout.visibility = View.VISIBLE

                    //Initialization
                    when (student.scores!!["$selectedWeek"]) {
                        100.0 -> holder.ui.btnA.isChecked = true
                        80.0 -> holder.ui.btnB.isChecked = true
                        70.0 -> holder.ui.btnC.isChecked = true
                        60.0 -> holder.ui.btnD.isChecked = true
                        0.0 -> holder.ui.btnF.isChecked = true
                    }
                    //https://developer.android.com/reference/kotlin/android/widget/RadioGroup#setoncheckedchangelistener
                    //https://developer.android.com/reference/kotlin/android/widget/RadioGroup.OnCheckedChangeListener
                    //Update Score
                    holder.ui.btnGroupA.setOnCheckedChangeListener(RadioGroup.OnCheckedChangeListener { _, checkedID ->
                        if (holder.ui.btnGroupA.findViewById<RadioButton>(checkedID).isPressed) {
                            var score = 0.0
                            when (checkedID) {
                                R.id.btnA -> score = 100.0
                                R.id.btnB -> score = 80.0
                                R.id.btnC -> score = 70.0
                                R.id.btnD -> score = 60.0
                                R.id.btnF -> score = 0.0
                            }
                            studentsCollection.document("${student.id}")
                                .update(mapOf("scores.$selectedWeek" to score))
                                .addOnSuccessListener {
                                    student.scores!!["$selectedWeek"] = score
                                    Toast.makeText(
                                        holder.ui.root.context,
                                        "${student.name} score updated successfully.",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                        }
                    })
                }
            }
        }
    }

    /****  ActionBar (Menu) Part ****/
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_marking, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here.
        return when (item.itemId) {
            R.id.app_bar_management -> return manageStudents()
            R.id.app_bar_summary -> return navigateToWeeklySummary()
            else -> super.onOptionsItemSelected(item)
        }
    }

    //Navigate to Student list view
    private fun manageStudents(): Boolean {
        var intent = Intent(this, StudentListActivity::class.java)
        startActivity(intent)
        return true
    }

    //Navigate to Weekly Summary View
    private fun navigateToWeeklySummary(): Boolean {
        var intent = Intent(this, WeeklySummaryActivity::class.java)
        startActivity(intent)
        return true
    }
}