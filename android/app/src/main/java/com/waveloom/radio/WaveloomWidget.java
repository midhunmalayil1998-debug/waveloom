package com.waveloom.radio;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

public class WaveloomWidget extends AppWidgetProvider {
    public static final String ACTION_WIDGET_CMD = "com.waveloom.radio.WIDGET_CMD";
    public static String currentStation = "Tap to open Waveloom";

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }

    public static void updateAllWidgets(Context context, String stationName) {
        currentStation = stationName;
        AppWidgetManager mgr = AppWidgetManager.getInstance(context);
        int[] ids = mgr.getAppWidgetIds(new ComponentName(context, WaveloomWidget.class));
        for (int id : ids) {
            updateAppWidget(context, mgr, id);
        }
    }

    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.waveloom_widget);
        views.setTextViewText(R.id.widget_station_name, currentStation);

        views.setOnClickPendingIntent(R.id.widget_station_name, getOpenAppIntent(context));
        views.setOnClickPendingIntent(R.id.widget_prev_btn, getCmdIntent(context, "prev", 1));
        views.setOnClickPendingIntent(R.id.widget_play_btn, getCmdIntent(context, "toggle", 2));
        views.setOnClickPendingIntent(R.id.widget_next_btn, getCmdIntent(context, "next", 3));

        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    private static PendingIntent getOpenAppIntent(Context context) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        return PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    private static PendingIntent getCmdIntent(Context context, String cmd, int reqCode) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.setAction(ACTION_WIDGET_CMD);
        intent.putExtra("cmd", cmd);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        return PendingIntent.getActivity(context, reqCode, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }
}
