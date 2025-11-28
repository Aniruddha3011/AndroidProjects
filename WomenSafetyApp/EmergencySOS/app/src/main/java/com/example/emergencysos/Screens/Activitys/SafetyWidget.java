package com.example.emergencysos.Screens.Activitys;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

import com.example.emergencysos.R;
import com.example.emergencysos.Screens.Safety_check_in;

public class SafetyWidget extends AppWidgetProvider {

    public static final String ACTION_SOS = "WIDGET_SOS";
    public static final String ACTION_CHECKIN = "WIDGET_CHECKIN";
    public static final String ACTION_START = "WIDGET_START";
    public static final String ACTION_STOP = "WIDGET_STOP";

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {

            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.activity_safety_widget);

            // SOS Button
            Intent sosIntent = new Intent(context, SafetyWidget.class);
            sosIntent.setAction(ACTION_SOS);
            PendingIntent sosPending = PendingIntent.getBroadcast(
                    context, 0, sosIntent, PendingIntent.FLAG_IMMUTABLE
            );
            views.setOnClickPendingIntent(R.id.btnWidgetSOS, sosPending);

            // Quick Check-In
            Intent checkIntent = new Intent(context, SafetyWidget.class);
            checkIntent.setAction(ACTION_CHECKIN);
            PendingIntent checkPending = PendingIntent.getBroadcast(
                    context, 1, checkIntent, PendingIntent.FLAG_IMMUTABLE
            );
            views.setOnClickPendingIntent(R.id.btnWidgetCheckIn, checkPending);

            // Start Check-In
            Intent startIntent = new Intent(context, SafetyWidget.class);
            startIntent.setAction(ACTION_START);
            PendingIntent startPending = PendingIntent.getBroadcast(
                    context, 2, startIntent, PendingIntent.FLAG_IMMUTABLE
            );
            views.setOnClickPendingIntent(R.id.btnWidgetStart, startPending);

            // Stop Check-In
            Intent stopIntent = new Intent(context, SafetyWidget.class);
            stopIntent.setAction(ACTION_STOP);
            PendingIntent stopPending = PendingIntent.getBroadcast(
                    context, 3, stopIntent, PendingIntent.FLAG_IMMUTABLE
            );
            views.setOnClickPendingIntent(R.id.btnWidgetStop, stopPending);

            appWidgetManager.updateAppWidget(appWidgetId, views);
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);

        String action = intent.getAction();
        Intent activityIntent = new Intent(context, Safety_check_in.class);
        activityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        switch (action) {

            case ACTION_SOS:
                activityIntent.putExtra("trigger", "sos");
                context.startActivity(activityIntent);
                break;

            case ACTION_CHECKIN:
                activityIntent.putExtra("trigger", "checkin");
                context.startActivity(activityIntent);
                break;

            case ACTION_START:
                activityIntent.putExtra("trigger", "start");
                context.startActivity(activityIntent);
                break;

            case ACTION_STOP:
                activityIntent.putExtra("trigger", "stop");
                context.startActivity(activityIntent);
                break;
        }
    }
}
