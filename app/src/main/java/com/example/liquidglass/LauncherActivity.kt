package com.example.liquidglass

import android.app.Activity
import android.content.Intent
import android.content.pm.ResolveInfo
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import java.text.DateFormat
import java.util.Date
import java.util.Locale

/**
 * Lightweight Android HOME activity.
 *
 * It intentionally uses opaque/translucent fills instead of live backdrop blur
 * so the launcher stays responsive on mid-range devices.
 */
class LauncherActivity : Activity() {

    private data class LaunchableApp(
        val label: String,
        val packageName: String,
        val className: String,
        val icon: android.graphics.drawable.Drawable
    )

    private lateinit var appGrid: LinearLayout
    private lateinit var searchField: EditText
    private var installedApps: List<LaunchableApp> = emptyList()

    private val columns: Int
        get() = if (resources.configuration.screenWidthDp >= 600) 6 else 4

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        actionBar?.hide()

        window.statusBarColor = Color.rgb(14, 20, 34)
        window.navigationBarColor = Color.rgb(14, 20, 34)

        installedApps = loadLaunchableApps()
        setContentView(createContent())
        renderApps(installedApps)
    }

    override fun onResume() {
        super.onResume()
        if (::appGrid.isInitialized) {
            installedApps = loadLaunchableApps()
            filterApps(searchField.text?.toString().orEmpty())
        }
    }

    private fun createContent(): View {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(22), dp(24), dp(22), dp(18))
            setBackgroundColor(Color.rgb(14, 20, 34))
        }

        val clock = TextView(this).apply {
            text = DateFormat.getTimeInstance(DateFormat.SHORT, Locale.getDefault()).format(Date())
            setTextColor(Color.WHITE)
            textSize = 42f
            setTypeface(typeface, Typeface.BOLD)
        }
        root.addView(clock, matchWidthWrap())

        val date = TextView(this).apply {
            text = DateFormat.getDateInstance(DateFormat.FULL, Locale.getDefault()).format(Date())
            setTextColor(0xFFB8C3D9.toInt())
            textSize = 14f
            setPadding(0, dp(2), 0, dp(20))
        }
        root.addView(date, matchWidthWrap())

        searchField = EditText(this).apply {
            hint = getString(R.string.launcher_search_hint)
            setSingleLine(true)
            textSize = 16f
            setTextColor(Color.WHITE)
            setHintTextColor(0xFF9AA8C0.toInt())
            setPadding(dp(16), 0, dp(16), 0)
            background = roundedBackground(0xFF202C43.toInt(), 22)
            addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    filterApps(s?.toString().orEmpty())
                }
                override fun afterTextChanged(s: Editable?) = Unit
            })
        }
        root.addView(searchField, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, dp(52)
        ).apply { bottomMargin = dp(20) })

        val sectionTitle = TextView(this).apply {
            text = getString(R.string.launcher_apps_title)
            setTextColor(Color.WHITE)
            textSize = 18f
            setTypeface(typeface, Typeface.BOLD)
            setPadding(0, 0, 0, dp(12))
        }
        root.addView(sectionTitle, matchWidthWrap())

        val scroll = ScrollView(this).apply {
            isFillViewport = true
            clipToPadding = false
        }
        appGrid = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        scroll.addView(appGrid, ScrollView.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ))
        root.addView(scroll, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f
        ))

        val footer = TextView(this).apply {
            text = getString(R.string.launcher_footer)
            setTextColor(0xFF71809B.toInt())
            textSize = 12f
            gravity = Gravity.CENTER
            setPadding(0, dp(12), 0, 0)
        }
        root.addView(footer, matchWidthWrap())
        return root
    }

    private fun loadLaunchableApps(): List<LaunchableApp> {
        val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        return packageManager.queryIntentActivities(launcherIntent, 0)
            .mapNotNull { info ->
                val activityInfo = info.activityInfo ?: return@mapNotNull null
                LaunchableApp(
                    label = info.loadLabel(packageManager).toString(),
                    packageName = activityInfo.packageName,
                    className = activityInfo.name,
                    icon = info.loadIcon(packageManager)
                )
            }
            .distinctBy { it.packageName to it.className }
            .sortedBy { it.label.lowercase(Locale.getDefault()) }
    }

    private fun filterApps(query: String) {
        val filtered = if (query.isBlank()) installedApps else installedApps.filter {
            it.label.contains(query.trim(), ignoreCase = true)
        }
        renderApps(filtered)
    }

    private fun renderApps(apps: List<LaunchableApp>) {
        if (!::appGrid.isInitialized) return
        appGrid.removeAllViews()

        if (apps.isEmpty()) {
            val empty = TextView(this).apply {
                text = getString(R.string.launcher_no_apps)
                setTextColor(0xFFB8C3D9.toInt())
                textSize = 15f
                gravity = Gravity.CENTER
                setPadding(0, dp(32), 0, dp(32))
            }
            appGrid.addView(empty, matchWidthWrap())
            return
        }

        apps.chunked(columns).forEach { rowApps ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.TOP
            }
            rowApps.forEach { app ->
                row.addView(createAppTile(app), LinearLayout.LayoutParams(0, dp(112), 1f))
            }
            repeat(columns - rowApps.size) {
                row.addView(View(this), LinearLayout.LayoutParams(0, dp(112), 1f))
            }
            appGrid.addView(row, matchWidthWrap())
        }
    }

    private fun createAppTile(app: LaunchableApp): View {
        val tile = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            isClickable = true
            isFocusable = true
            contentDescription = app.label
            setPadding(dp(3), dp(6), dp(3), dp(6))
            setOnClickListener {
                val intent = Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_LAUNCHER)
                    setClassName(app.packageName, app.className)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                runCatching { startActivity(intent) }
            }
        }

        val icon = ImageView(this).apply {
            setImageDrawable(app.icon)
            scaleType = ImageView.ScaleType.FIT_CENTER
        }
        tile.addView(icon, LinearLayout.LayoutParams(dp(48), dp(48)))

        val label = TextView(this).apply {
            text = app.label
            setTextColor(Color.WHITE)
            textSize = 11f
            gravity = Gravity.CENTER
            maxLines = 2
            ellipsize = android.text.TextUtils.TruncateAt.END
            setPadding(0, dp(6), 0, 0)
        }
        tile.addView(label, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ))
        return tile
    }

    private fun roundedBackground(color: Int, radiusDp: Int) = GradientDrawable().apply {
        setColor(color)
        cornerRadius = dp(radiusDp).toFloat()
    }

    private fun matchWidthWrap() = LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
    )

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density + 0.5f).toInt()
}
