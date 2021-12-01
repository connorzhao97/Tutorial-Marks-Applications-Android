package au.edu.utas.yucongz.assignment2

class Score(
    var week1: Double? = 0.0,
    var week2: Double? = 0.0,
    var week3: Double? = 0.0,
    var week4: Double? = 0.0,
    var week5: Double? = 0.0,
    var week6: Double? = 0.0,
    var week7: Double? = 0.0,
    var week8: Double? = 0.0,
    var week9: Double? = 0.0,
    var week10: Double? = 0.0,
    var week11: Double? = 0.0,
    var week12: Double? = 0.0
) {
    //https://kotlinlang.org/docs/properties.html#getters-and-setters
    //https://kotlinlang.org/docs/operator-overloading.html
    operator fun get(s: String): Double {
        var score = 0.0
        when (s) {
            "week1" -> score = week1!!
            "week2" -> score = week2!!
            "week3" -> score = week3!!
            "week4" -> score = week4!!
            "week5" -> score = week5!!
            "week6" -> score = week6!!
            "week7" -> score = week7!!
            "week8" -> score = week8!!
            "week9" -> score = week9!!
            "week10" -> score = week10!!
            "week11" -> score = week11!!
            "week12" -> score = week12!!
        }
        return score
    }

    operator fun get(position: Int): Double {
        var score = 0.0
        when (position) {
            0 -> score = week1!!
            1 -> score = week2!!
            2 -> score = week3!!
            3 -> score = week4!!
            4 -> score = week5!!
            5 -> score = week6!!
            6 -> score = week7!!
            7 -> score = week8!!
            8 -> score = week9!!
            9 -> score = week10!!
            10 -> score = week11!!
            11 -> score = week12!!
        }
        return score
    }

    operator fun set(s: String, score: Double) {
        when (s) {
            "week1" -> week1 = score
            "week2" -> week2 = score
            "week3" -> week3 = score
            "week4" -> week4 = score
            "week5" -> week5 = score
            "week6" -> week6 = score
            "week7" -> week7 = score
            "week8" -> week8 = score
            "week9" -> week9 = score
            "week10" -> week10 = score
            "week11" -> week11 = score
            "week12" -> week12 = score
        }
    }

    fun getWeek(position: Int): String {
        var week = ""
        when (position) {
            0 -> week = "week1"
            1 -> week = "week2"
            2 -> week = "week3"
            3 -> week = "week4"
            4 -> week = "week5"
            5 -> week = "week6"
            6 -> week = "week7"
            7 -> week = "week8"
            8 -> week = "week9"
            9 -> week = "week10"
            10 -> week = "week11"
            11 -> week = "week12"
        }
        return week
    }
}