package au.edu.utas.yucongz.assignment2

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import au.edu.utas.yucongz.assignment2.databinding.ActivityWeeklySummaryBinding
import au.edu.utas.yucongz.assignment2.databinding.WeeklySummaryItemBinding

class WeeklySummaryActivity : AppCompatActivity() {

    private lateinit var ui: ActivityWeeklySummaryBinding
    private var selectedWeek: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ui = ActivityWeeklySummaryBinding.inflate(layoutInflater)
        setContentView(ui.root)

        //https://developer.android.com/reference/kotlin/android/app/ActionBar#setdisplayhomeasupenabled
        //https://developer.android.com/reference/kotlin/android/app/ActionBar#setdisplayshowhomeenabled
        setSupportActionBar(ui.toolbarWeeklySummary)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        //Holder and Adapter
        ui.weeklySummaryList.adapter = StudentAdapter(students = students)
        ui.weeklySummaryList.layoutManager = LinearLayoutManager(this)


        ui.spinnerWeeklySummary.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
                    selectedWeek = p0?.getItemAtPosition(p2).toString()
                    var summaryScore = 0.0
                    for (student in students) {
                        summaryScore += student.scores!!["$selectedWeek"]
                    }
                    if (students.size != 0) {
                        ui.txtWeeklySummaryScore.text =
                            (String.format("%.2f", (summaryScore / students.size)))
                    } else {
                        ui.txtWeeklySummaryScore.text = "0.00"
                    }

                    ui.txtWeeklySummaryScheme.text = markingSchemes[selectedWeek!!]
                    (ui.weeklySummaryList.adapter as StudentAdapter).notifyDataSetChanged()
                }

                override fun onNothingSelected(p0: AdapterView<*>?) {}
            }
    }

    /****  RecyclerView Part ****/
    inner class StudentHolder(var ui: WeeklySummaryItemBinding) : RecyclerView.ViewHolder(ui.root) {}

    inner class StudentAdapter(private val students: MutableList<Student>) : RecyclerView.Adapter<StudentHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StudentHolder {
            val ui = WeeklySummaryItemBinding.inflate(layoutInflater, parent, false)
            return StudentHolder(ui)
        }

        override fun onBindViewHolder(holder: StudentHolder, position: Int) {
            val student = students[position]
            holder.ui.txtWeeklySummaryStudentName.text = student.name
            holder.ui.txtWeeklySummaryStudentID.text = student.studentID.toString()
            holder.ui.txtWeeklyScore.text = student.scores?.get("$selectedWeek").toString()
        }

        override fun getItemCount(): Int {
            return students.size
        }
    }
}