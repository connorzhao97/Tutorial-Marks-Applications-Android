package au.edu.utas.yucongz.assignment2

class MarkingScheme(
    var id: String? = null,
    var week1: String = "Attendance",
    var week2: String = "Attendance",
    var week3: String = "Attendance",
    var week4: String = "Attendance",
    var week5: String = "Attendance",
    var week6: String = "Attendance",
    var week7: String = "Attendance",
    var week8: String = "Attendance",
    var week9: String = "Attendance",
    var week10: String = "Attendance",
    var week11: String = "Attendance",
    var week12: String = "Attendance"
) {
    //https://kotlinlang.org/docs/properties.html#getters-and-setters
    //https://kotlinlang.org/docs/operator-overloading.html
    operator fun get(selectedWeek: String): String {
        var markingScheme = ""
        when (selectedWeek) {
            "week1" -> markingScheme = week1
            "week2" -> markingScheme = week2
            "week3" -> markingScheme = week3
            "week4" -> markingScheme = week4
            "week5" -> markingScheme = week5
            "week6" -> markingScheme = week6
            "week7" -> markingScheme = week7
            "week8" -> markingScheme = week8
            "week9" -> markingScheme = week9
            "week10" -> markingScheme = week10
            "week11" -> markingScheme = week11
            "week12" -> markingScheme = week12
        }
        return markingScheme
    }

    operator fun set(selectedWeek: String, value: String) {
        when (selectedWeek) {
            "week1" -> week1 = value
            "week2" -> week2 = value
            "week3" -> week3 = value
            "week4" -> week4 = value
            "week5" -> week5 = value
            "week6" -> week6 = value
            "week7" -> week7 = value
            "week8" -> week8 = value
            "week9" -> week9 = value
            "week10" -> week10 = value
            "week11" -> week11 = value
            "week12" -> week12 = value
        }
    }
}
