package com.finora.android.core.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.RemoteViews
import com.finora.android.MainActivity
import com.finora.android.R
import com.finora.android.core.di.DatabaseModule
import com.finora.android.core.model.Amount
import com.finora.android.core.model.AppCurrency
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar

class FinoraAppWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        DatabaseModule.initialize(context)

        CoroutineScope(Dispatchers.IO).launch {
            val db = DatabaseModule.getDatabase()
            val profile = db.profileDao().getActiveProfile()
            val currencySymbol = if (profile != null) AppCurrency.fromCode(profile.currencyCode).symbol else "₹"

            val cal = Calendar.getInstance()
            cal.set(Calendar.DAY_OF_MONTH, 1)
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            val startOfMonth = cal.timeInMillis

            val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
            cal.set(Calendar.DAY_OF_MONTH, daysInMonth)
            cal.set(Calendar.HOUR_OF_DAY, 23)
            cal.set(Calendar.MINUTE, 59)
            cal.set(Calendar.SECOND, 59)
            cal.set(Calendar.MILLISECOND, 999)
            val endOfMonth = cal.timeInMillis

            val totalSpentMinor = if (profile != null) {
                db.expenseDao().getTotalSpendInDateRange(profile.id, startOfMonth, endOfMonth)
            } else 0L

            val formattedSpent = "$currencySymbol${Amount(totalSpentMinor).toFormattedString()}"

            for (widgetId in appWidgetIds) {
                val views = RemoteViews(context.packageName, R.layout.widget_finora_summary)
                views.setTextViewText(R.id.widget_month_spend, formattedSpent)
                views.setTextViewText(R.id.widget_budget_status, "Active Tracking")

                // Open App Intent
                val openAppIntent = Intent(context, MainActivity::class.java)
                val openAppPendingIntent = PendingIntent.getActivity(
                    context,
                    0,
                    openAppIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.widget_container, openAppPendingIntent)

                // Quick Add Expense Intent
                val addExpenseIntent = Intent(Intent.ACTION_VIEW, Uri.parse("finora://add_expense"), context, MainActivity::class.java)
                val addExpensePendingIntent = PendingIntent.getActivity(
                    context,
                    1,
                    addExpenseIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.widget_btn_add, addExpensePendingIntent)

                appWidgetManager.updateAppWidget(widgetId, views)
            }
        }
    }

    companion object {
        fun triggerUpdate(context: Context) {
            val intent = Intent(context, FinoraAppWidgetProvider::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            }
            val ids = AppWidgetManager.getInstance(context).getAppWidgetIds(
                ComponentName(context, FinoraAppWidgetProvider::class.java)
            )
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            context.sendBroadcast(intent)
        }
    }
}
