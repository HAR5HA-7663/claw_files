package com.ppl.tracker

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.CompoundButton
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

/**
 * PPL Tracker -- single-activity offline workout tracker.
 * UI is built entirely in code (no AndroidX / Compose) so it builds against
 * the bare Android framework.
 */
class MainActivity : Activity() {

    private lateinit var prefs: SharedPreferences

    // --- palette (dark) ---
    private val colBg = Color.parseColor("#0E1013")
    private val colCard = Color.parseColor("#191D23")
    private val colCardDone = Color.parseColor("#14251A")
    private val colAccent = Color.parseColor("#FF6D3F")
    private val colText = Color.parseColor("#F2F4F7")
    private val colSub = Color.parseColor("#9AA4B2")
    private val colLast = Color.parseColor("#5EC26A")
    private val colField = Color.parseColor("#0B0D10")

    private var selectedWeek = 1        // 1..4
    private var selectedWeekday = 0     // 0=Mon .. 6=Sun
    private var suppressSpinner = false

    private lateinit var weekSpinner: Spinner
    private lateinit var daySpinner: Spinner
    private lateinit var content: LinearLayout   // re-rendered body
    private lateinit var headerTitle: TextView
    private lateinit var headerSub: TextView

    private val dateKeyFmt: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd")
    private val prettyFmt: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE, d MMM yyyy")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = getSharedPreferences("ppl_tracker", Context.MODE_PRIVATE)

        // default selection = today's slot in the cycle
        val pos = cyclePosition(LocalDate.now())
        selectedWeek = pos / 7 + 1
        selectedWeekday = pos % 7

        setContentView(buildRoot())
        syncSpinners()
        render()
    }

    // ---------------------------------------------------------------- cycle math

    private fun anchor(): LocalDate =
        LocalDate.of(Program.ANCHOR_YEAR, Program.ANCHOR_MONTH, Program.ANCHOR_DAY)

    /** Position within the 28-day cycle for a date: 0..27 (0 = Week1 Monday). */
    private fun cyclePosition(date: LocalDate): Int {
        val days = ChronoUnit.DAYS.between(anchor(), date)
        return Math.floorMod(days, 28L).toInt()
    }

    /** Monday of the current cycle that contains [date]. */
    private fun cycleStartMonday(date: LocalDate): LocalDate =
        date.minusDays(cyclePosition(date).toLong())

    /** Concrete calendar date for the currently-selected week/day slot. */
    private fun selectedDate(): LocalDate =
        cycleStartMonday(LocalDate.now())
            .plusDays(((selectedWeek - 1) * 7 + selectedWeekday).toLong())

    private fun isToday(): Boolean = selectedDate() == LocalDate.now()

    // ---------------------------------------------------------------- UI scaffold

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    private fun buildRoot(): View {
        val scroll = ScrollView(this).apply {
            setBackgroundColor(colBg)
            isFillViewport = true
        }
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(20), dp(16), dp(28))
        }

        // App title
        root.addView(TextView(this).apply {
            text = "🏋️  PPL Tracker"
            setTextColor(colText)
            textSize = 26f
            setTypeface(typeface, Typeface.BOLD)
        })

        headerSub = TextView(this).apply {
            setTextColor(colSub)
            textSize = 14f
            setPadding(0, dp(2), 0, dp(14))
        }
        root.addView(headerSub)

        // Week / Day picker card
        root.addView(buildPickerCard())

        // Big day title
        headerTitle = TextView(this).apply {
            setTextColor(colAccent)
            textSize = 22f
            setTypeface(typeface, Typeface.BOLD)
            setPadding(dp(2), dp(18), 0, dp(2))
        }
        root.addView(headerTitle)

        // Body container (re-rendered on selection change)
        content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root.addView(content)

        scroll.addView(
            root,
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )
        return scroll
    }

    private fun buildPickerCard(): View {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = rounded(colCard, dp(14))
            setPadding(dp(12), dp(10), dp(12), dp(10))
        }

        weekSpinner = Spinner(this).apply {
            adapter = darkAdapter((1..Program.TOTAL_WEEKS).map { "Week $it" })
            onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                    if (suppressSpinner) return
                    selectedWeek = pos + 1
                    render()
                }
                override fun onNothingSelected(p: AdapterView<*>?) {}
            }
        }
        daySpinner = Spinner(this).apply {
            adapter = darkAdapter(Program.weekdayNamesLong.toList())
            onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                    if (suppressSpinner) return
                    selectedWeekday = pos
                    render()
                }
                override fun onNothingSelected(p: AdapterView<*>?) {}
            }
        }

        val lpWeek = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        val lpDay = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.3f)
        card.addView(weekSpinner, lpWeek)
        card.addView(daySpinner, lpDay)

        val todayBtn = Button(this).apply {
            text = "Today"
            isAllCaps = false
            textSize = 15f
            setTextColor(Color.WHITE)
            background = rounded(colAccent, dp(10))
            setPadding(dp(16), dp(6), dp(16), dp(6))
            setOnClickListener {
                val pos = cyclePosition(LocalDate.now())
                selectedWeek = pos / 7 + 1
                selectedWeekday = pos % 7
                syncSpinners()
                render()
            }
        }
        card.addView(
            todayBtn,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { leftMargin = dp(10) }
        )
        return card
    }

    private fun darkAdapter(items: List<String>): ArrayAdapter<String> {
        return object : ArrayAdapter<String>(
            this, android.R.layout.simple_spinner_item, items
        ) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val tv = super.getView(position, convertView, parent) as TextView
                tv.setTextColor(colText); tv.textSize = 16f; return tv
            }
            override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                val tv = super.getDropDownView(position, convertView, parent) as TextView
                tv.setTextColor(colText); tv.setBackgroundColor(colCard)
                tv.setPadding(dp(16), dp(14), dp(16), dp(14)); tv.textSize = 16f; return tv
            }
        }.also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
    }

    private fun syncSpinners() {
        suppressSpinner = true
        weekSpinner.setSelection(selectedWeek - 1)
        daySpinner.setSelection(selectedWeekday)
        suppressSpinner = false
    }

    private fun rounded(color: Int, radius: Int): GradientDrawable =
        GradientDrawable().apply {
            setColor(color)
            cornerRadius = radius.toFloat()
        }

    // ---------------------------------------------------------------- render body

    private fun render() {
        content.removeAllViews()
        val date = selectedDate()
        headerSub.text = "${if (isToday()) "Today · " else ""}${date.format(prettyFmt)}"

        val dayType = Program.dayTypeFor(selectedWeekday)
        if (dayType == null) {
            headerTitle.text = "Rest Day 💤"
            content.addView(restCard())
            return
        }

        headerTitle.text = "Week $selectedWeek — ${dayType.label}: ${dayType.focus}"
        val exercises = Program.exercisesFor(selectedWeek, dayType)
        val dateKey = date.format(dateKeyFmt)

        // progress line
        val progress = TextView(this).apply {
            setTextColor(colSub)
            textSize = 14f
            setPadding(dp(2), 0, 0, dp(10))
        }
        content.addView(progress)
        updateProgress(progress, dateKey, exercises)

        for (exercise in exercises) {
            content.addView(exerciseCard(exercise, dateKey, progress, exercises))
        }
    }

    private fun updateProgress(tv: TextView, dateKey: String, exercises: List<Exercise>) {
        val done = exercises.count { prefs.getBoolean(checkKey(dateKey, it.name), false) }
        tv.text = "✓  $done / ${exercises.size} done"
    }

    private fun restCard(): View {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = rounded(colCard, dp(16))
            setPadding(dp(20), dp(24), dp(20), dp(24))
        }
        card.addView(TextView(this).apply {
            text = "Recovery is where you grow."
            setTextColor(colText); textSize = 20f
            setTypeface(typeface, Typeface.BOLD)
        })
        card.addView(TextView(this).apply {
            text = "No lifting today. Take it easy — stretch, walk, hydrate, and get good sleep so you come back stronger."
            setTextColor(colSub); textSize = 16f
            setPadding(0, dp(10), 0, 0)
            setLineSpacing(dp(4).toFloat(), 1f)
        })
        return card
    }

    private fun exerciseCard(
        exercise: Exercise,
        dateKey: String,
        progress: TextView,
        allExercises: List<Exercise>
    ): View {
        val checked = prefs.getBoolean(checkKey(dateKey, exercise.name), false)

        val card = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            background = rounded(if (checked) colCardDone else colCard, dp(14))
            setPadding(dp(12), dp(14), dp(14), dp(14))
        }

        // checkbox
        val cb = CheckBox(this).apply {
            isChecked = checked
            scaleX = 1.3f; scaleY = 1.3f
        }

        // middle: name + scheme + last
        val mid = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        val nameTv = TextView(this).apply {
            text = exercise.name
            setTextColor(colText); textSize = 19f
            setTypeface(typeface, Typeface.BOLD)
        }
        val schemeTv = TextView(this).apply {
            text = exercise.scheme
            setTextColor(colAccent); textSize = 16f
            setPadding(0, dp(2), 0, 0)
        }
        val lastTv = TextView(this).apply {
            setTextColor(colLast); textSize = 14f
            setPadding(0, dp(4), 0, 0)
        }
        val last = lastWeight(exercise.name, dateKey)
        if (last != null) {
            lastTv.text = "Last: $last kg"
            lastTv.visibility = View.VISIBLE
        } else {
            lastTv.visibility = View.GONE
        }
        mid.addView(nameTv); mid.addView(schemeTv); mid.addView(lastTv)

        // weight field
        val weightEt = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            hint = "kg"
            setHintTextColor(colSub)
            setTextColor(colText)
            textSize = 18f
            gravity = Gravity.CENTER
            background = rounded(colField, dp(10))
            setPadding(dp(10), dp(10), dp(10), dp(10))
            setText(prefs.getString(weightKey(dateKey, exercise.name), ""))
        }
        weightEt.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val v = s?.toString()?.trim() ?: ""
                prefs.edit().putString(weightKey(dateKey, exercise.name), v).apply()
            }
        })

        cb.setOnCheckedChangeListener(object : CompoundButton.OnCheckedChangeListener {
            override fun onCheckedChanged(button: CompoundButton?, isChecked: Boolean) {
                prefs.edit().putBoolean(checkKey(dateKey, exercise.name), isChecked).apply()
                card.background = rounded(if (isChecked) colCardDone else colCard, dp(14))
                updateProgress(progress, dateKey, allExercises)
            }
        })

        val lpCb = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply { gravity = Gravity.CENTER_VERTICAL; rightMargin = dp(10); leftMargin = dp(4) }
        val lpMid = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            .apply { gravity = Gravity.CENTER_VERTICAL }
        val lpEt = LinearLayout.LayoutParams(dp(78), ViewGroup.LayoutParams.WRAP_CONTENT)
            .apply { gravity = Gravity.CENTER_VERTICAL; leftMargin = dp(8) }

        card.addView(cb, lpCb)
        card.addView(mid, lpMid)
        card.addView(weightEt, lpEt)

        val outer = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = dp(10) }
        card.layoutParams = outer
        return card
    }

    // ---------------------------------------------------------------- persistence

    private fun checkKey(dateKey: String, ex: String) = "chk:$dateKey:$ex"
    private fun weightKey(dateKey: String, ex: String) = "wt:$dateKey:$ex"

    /** Most recent weight logged for [ex] on a date strictly before [beforeKey]. */
    private fun lastWeight(ex: String, beforeKey: String): String? {
        val prefix = "wt:"
        val suffix = ":$ex"
        var bestDate = ""
        var bestVal: String? = null
        for ((k, v) in prefs.all) {
            if (!k.startsWith(prefix) || !k.endsWith(suffix)) continue
            val dk = k.substring(prefix.length, k.length - suffix.length)
            if (dk.length != 8 || dk >= beforeKey) continue
            val sv = v as? String ?: continue
            if (sv.isBlank()) continue
            if (dk > bestDate) { bestDate = dk; bestVal = sv }
        }
        return bestVal
    }
}
