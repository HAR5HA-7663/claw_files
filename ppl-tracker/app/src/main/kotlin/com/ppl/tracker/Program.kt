package com.ppl.tracker

/**
 * Embedded 4-week Push/Pull/Legs (5-day) program.
 * All data is compiled into the app -- no file import, fully offline.
 */

/** A single exercise: display name + "sets x reps" scheme. */
data class Exercise(val name: String, val scheme: String)

/** The five training day types. Order matches Mon..Fri. */
enum class DayType(val label: String, val focus: String) {
    PUSH_A("Push A", "Chest Focus"),
    PULL_A("Pull A", "Back Focus"),
    LEGS("Legs", "Leg Day"),
    PUSH_B("Push B", "Shoulder Focus"),
    PULL_B("Pull B", "Posterior Chain");
}

object Program {

    /** Monday of Week 1 -- the anchor the whole cycle is computed from. */
    const val ANCHOR_YEAR = 2026
    const val ANCHOR_MONTH = 7
    const val ANCHOR_DAY = 13   // Monday, July 13 2026

    const val TOTAL_WEEKS = 4

    private fun ex(name: String, scheme: String) = Exercise(name, scheme)

    // weeks[weekIndex 0..3][DayType] -> list of exercises
    private val weeks: Array<Map<DayType, List<Exercise>>> = arrayOf(
        // ---------------- WEEK 1 ----------------
        mapOf(
            DayType.PUSH_A to listOf(
                ex("Barbell Bench Press", "4 x 6-8"),
                ex("Incline DB Press", "3 x 8-10"),
                ex("Overhead Shoulder Press", "3 x 8-10"),
                ex("Cable Flyes", "3 x 12-15"),
                ex("Rope Pushdowns", "3 x 12-15"),
            ),
            DayType.PULL_A to listOf(
                ex("Lat Pulldown or Pull-ups", "4 x 8-10"),
                ex("Barbell Row", "3 x 8-10"),
                ex("Seated Cable Row", "3 x 10-12"),
                ex("Face Pulls", "3 x 15"),
                ex("Barbell Curls", "3 x 10-12"),
            ),
            DayType.LEGS to listOf(
                ex("Barbell Squat", "4 x 6-8"),
                ex("Leg Press", "3 x 10-12"),
                ex("Leg Extensions", "3 x 12-15"),
                ex("Standing Calf Raises", "4 x 15"),
                ex("Walking Lunges", "3 x 10/leg"),
            ),
            DayType.PUSH_B to listOf(
                ex("Overhead Barbell Press", "4 x 6-8"),
                ex("Flat DB Press", "3 x 8-10"),
                ex("Lateral Raises", "4 x 12-15"),
                ex("Dips", "3 x 8-12"),
                ex("Overhead Tricep Extension", "3 x 12"),
            ),
            DayType.PULL_B to listOf(
                ex("Deadlift", "4 x 5-6"),
                ex("Chest-Supported Row", "3 x 8-10"),
                ex("Rear Delt Flyes", "3 x 15"),
                ex("Hammer Curls", "3 x 10-12"),
                ex("Preacher Curls", "3 x 12"),
            ),
        ),
        // ---------------- WEEK 2 ----------------
        mapOf(
            DayType.PUSH_A to listOf(
                ex("Incline Barbell Press", "4 x 6-8"),
                ex("Flat DB Press", "3 x 8-10"),
                ex("Machine Shoulder Press", "3 x 10"),
                ex("Pec Deck", "3 x 12-15"),
                ex("Skull Crushers", "3 x 10-12"),
            ),
            DayType.PULL_A to listOf(
                ex("Pull-ups", "4 x 8-10"),
                ex("T-Bar Row", "3 x 8-10"),
                ex("Single-Arm DB Row", "3 x 10/side"),
                ex("Reverse Pec Deck", "3 x 15"),
                ex("EZ-Bar Curls", "3 x 10-12"),
            ),
            DayType.LEGS to listOf(
                ex("Front Squat", "4 x 6-8"),
                ex("Hack Squat", "3 x 10"),
                ex("Romanian Deadlift", "3 x 8-10"),
                ex("Seated Calf Raises", "4 x 15"),
                ex("Leg Curls", "3 x 12"),
            ),
            DayType.PUSH_B to listOf(
                ex("Seated DB Shoulder Press", "4 x 8"),
                ex("Incline DB Press", "3 x 8-10"),
                ex("Cable Lateral Raises", "4 x 12-15"),
                ex("Close-Grip Bench", "3 x 8-10"),
                ex("Single-Arm Overhead Extension", "3 x 12/side"),
            ),
            DayType.PULL_B to listOf(
                ex("Rack Pulls", "4 x 5-6"),
                ex("Pendlay Row", "3 x 8"),
                ex("Cable Rear Delt Flyes", "3 x 15"),
                ex("Incline DB Curls", "3 x 10-12"),
                ex("Cable Hammer Curls", "3 x 12"),
            ),
        ),
        // ---------------- WEEK 3 ----------------
        mapOf(
            DayType.PUSH_A to listOf(
                ex("Flat DB Press", "4 x 8"),
                ex("Incline Smith Press", "3 x 8-10"),
                ex("Arnold Press", "3 x 10"),
                ex("Low-to-High Cable Flyes", "3 x 12-15"),
                ex("Dips (tricep focus)", "3 x 8-12"),
            ),
            DayType.PULL_A to listOf(
                ex("Wide-Grip Lat Pulldown", "4 x 8-10"),
                ex("Meadows Row", "3 x 8-10/side"),
                ex("Straight-Arm Pulldown", "3 x 12"),
                ex("Face Pulls", "3 x 15"),
                ex("Spider Curls", "3 x 12"),
            ),
            DayType.LEGS to listOf(
                ex("Squat (pause reps)", "4 x 5-6"),
                ex("Bulgarian Split Squats", "3 x 8/leg"),
                ex("Leg Extensions", "3 x 12-15"),
                ex("Standing Calf Raises", "4 x 15"),
                ex("Hip Thrusts", "3 x 10"),
            ),
            DayType.PUSH_B to listOf(
                ex("Barbell OHP", "4 x 6-8"),
                ex("Machine Chest Press", "3 x 10"),
                ex("DB Lateral Raises (drop set last)", "4 x 12-15"),
                ex("Cable Pushdowns", "3 x 12-15"),
                ex("Diamond Push-ups", "3 x AMRAP"),
            ),
            DayType.PULL_B to listOf(
                ex("Deadlift", "4 x 5-6"),
                ex("Chest-Supported Row", "3 x 8-10"),
                ex("Shrugs", "3 x 12"),
                ex("Concentration Curls", "3 x 12/side"),
                ex("Reverse Curls", "3 x 12"),
            ),
        ),
        // ---------------- WEEK 4 ----------------
        mapOf(
            DayType.PUSH_A to listOf(
                ex("Bench Press (heavy)", "5 x 5"),
                ex("Incline Cable Flyes", "3 x 12"),
                ex("DB Shoulder Press", "3 x 8-10"),
                ex("Machine Flyes", "3 x 12-15"),
                ex("Rope Pushdowns", "3 x 12-15"),
            ),
            DayType.PULL_A to listOf(
                ex("Weighted Pull-ups or Heavy Pulldown", "4 x 6-8"),
                ex("Barbell Row", "4 x 6-8"),
                ex("Wide-Grip Cable Row", "3 x 10"),
                ex("Rear Delt Flyes", "3 x 15"),
                ex("21s Curls", "3 rounds"),
            ),
            DayType.LEGS to listOf(
                ex("Leg Press (heavy)", "5 x 8"),
                ex("Front Squat", "3 x 6-8"),
                ex("Romanian Deadlift", "3 x 8"),
                ex("Leg Curls", "3 x 12"),
                ex("Calf Raises", "4 x 15"),
            ),
            DayType.PUSH_B to listOf(
                ex("Push Press", "4 x 5-6"),
                ex("Incline DB Press", "3 x 8-10"),
                ex("Lateral Raise Machine", "4 x 12-15"),
                ex("Weighted Dips", "3 x 6-8"),
                ex("Overhead Rope Extension", "3 x 12"),
            ),
            DayType.PULL_B to listOf(
                ex("Deficit Deadlift (or regular, heavier)", "4 x 4-5"),
                ex("Single-Arm Cable Row", "3 x 10/side"),
                ex("Face Pulls", "3 x 15"),
                ex("Hammer Curls", "3 x 10-12"),
                ex("Cable Curls", "3 x 12"),
            ),
        ),
    )

    /** Mon..Fri map to a DayType; Sat/Sun (index 5,6) are rest days (null). */
    private val weekdayToType: Array<DayType?> = arrayOf(
        DayType.PUSH_A, // Mon
        DayType.PULL_A, // Tue
        DayType.LEGS,   // Wed
        DayType.PUSH_B, // Thu
        DayType.PULL_B, // Fri
        null,           // Sat
        null,           // Sun
    )

    /** @param week 1..4, @param weekdayIndex 0=Mon..6=Sun. Null == rest day. */
    fun dayTypeFor(weekdayIndex: Int): DayType? = weekdayToType[weekdayIndex]

    fun exercisesFor(week: Int, dayType: DayType): List<Exercise> =
        weeks[week - 1][dayType] ?: emptyList()

    val weekdayNames = arrayOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    val weekdayNamesLong = arrayOf(
        "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"
    )
}
