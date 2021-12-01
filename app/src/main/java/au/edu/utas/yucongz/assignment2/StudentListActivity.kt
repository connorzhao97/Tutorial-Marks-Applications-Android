package au.edu.utas.yucongz.assignment2

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Menu
import android.view.MenuItem
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import au.edu.utas.yucongz.assignment2.databinding.ActivityStudentListBinding
import au.edu.utas.yucongz.assignment2.databinding.StudentListItemBinding
import java.io.File

const val STUDENT_DETAILS_INDEX = "Student_Details_Index"

class StudentListActivity : AppCompatActivity() {

    private lateinit var ui: ActivityStudentListBinding


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ui = ActivityStudentListBinding.inflate(layoutInflater)
        setContentView(ui.root)

        tempStudents.addAll(students)

        //https://developer.android.com/reference/kotlin/android/app/ActionBar#setdisplayhomeasupenabled
        //https://developer.android.com/reference/kotlin/android/app/ActionBar#setdisplayshowhomeenabled
        setSupportActionBar(ui.toolbarStudentList)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        ui.studentsList.layoutManager = LinearLayoutManager(this)
        ui.studentsList.adapter = StudentAdapter(students = tempStudents)

        //https://developer.android.com/reference/kotlin/android/widget/TextView#addtextchangedlistener
        //https://developer.android.com/reference/kotlin/android/text/TextWatcher.html
        ui.editTextStudentListSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                tempStudents.clear()
                //https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/filter.html#filter
                tempStudents.addAll(students.filter {
                    it.name!!.contains(p0!!.trim(), true) || it.studentID!!.toString().contains(p0!!.trim())
                } as MutableList<Student>)
                ui.studentsList.adapter?.notifyDataSetChanged()
            }

            override fun afterTextChanged(p0: Editable?) {}
        })
    }

    override fun onResume() {
        super.onResume()
        ui.studentsList.adapter?.notifyDataSetChanged()
    }

    override fun onDestroy() {
        super.onDestroy()
        tempStudents.clear()
    }

    /****  ActionBar (Menu) Part ****/
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_student_list, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here.
        return when (item.itemId) {
            R.id.app_bar_add_student -> return addStudent()
            R.id.app_bar_share_socres -> return shareAllScores()
            else -> super.onOptionsItemSelected(item)
        }
    }


    private fun addStudent(): Boolean {
        var intent = Intent(this, AddStudentActivity::class.java)
        startActivity(intent)
        return true
    }

    private fun shareAllScores(): Boolean {
        var shareContent = ""
        for (student in students) {
            var summaryScore = 0.0
            for (i in 0..11) {
                summaryScore += student.scores!![i]
            }
            shareContent += """
            Student Name: ${student.name},
            Student ID: ${student.studentID},
            Week1 Score: ${student.scores!!["week1"]},
            Week2 Score: ${student.scores!!["week2"]},
            Week3 Score: ${student.scores!!["week3"]},
            Week4 Score: ${student.scores!!["week4"]},
            Week5 Score: ${student.scores!!["week5"]},
            Week6 Score: ${student.scores!!["week6"]},
            Week7 Score: ${student.scores!!["week7"]},
            Week8 Score: ${student.scores!!["week8"]},
            Week9 Score: ${student.scores!!["week9"]},
            Week10 Score: ${student.scores!!["week10"]},
            Week11 Score: ${student.scores!!["week11"]},
            Week12 Score: ${student.scores!!["week12"]},
            Summary Score: $summaryScore/1200 (${String.format("%.2f", summaryScore / 12.0)}%).
            -----------------------------------
            
        """.trimIndent()
        }
        var sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, shareContent)
            type = "text/plain"
        }
        startActivity(Intent.createChooser(sendIntent, "Share via..."))
        return true
    }

    /****  RecyclerView Part ****/
    inner class StudentHolder(var ui: StudentListItemBinding) : RecyclerView.ViewHolder(ui.root) {}

    inner class StudentAdapter(private val students: MutableList<Student>) : RecyclerView.Adapter<StudentHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StudentHolder {
            val ui = StudentListItemBinding.inflate(layoutInflater, parent, false)
            return StudentHolder(ui)
        }

        override fun getItemCount(): Int {
            return students.size
        }

        override fun onBindViewHolder(holder: StudentHolder, position: Int) {
            val student = students[position]
            holder.ui.txtListItemName.text = student.name
            holder.ui.txtListItemID.text = student.studentID.toString()
            if (student.avatarUrl !== "") {
                holder.ui.avatarListItem.setImageURI(Uri.fromFile(File(student.avatarUrl)))
            } else {
                holder.ui.avatarListItem.setImageResource(R.drawable.ic_user)
            }
            var summaryScore = 0.0
            for (i in 0..11) {
                summaryScore += student.scores!![i]
            }
            holder.ui.txtListItemSummaryGrade.text = String.format("%.2f", (summaryScore / 12.0)) + "%"
            holder.ui.root.setOnClickListener {
                var intent = Intent(holder.ui.root.context, StudentDetailsActivity::class.java)
                intent.putExtra(STUDENT_DETAILS_INDEX, position)
                startActivity(intent)
            }
        }
    }
}