package com.example.taskmenadzer.ui.charts;

import android.graphics.Color;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;

public class ChartFactory {

    public static void configureLineChart(LineChart chart) {
        chart.getDescription().setEnabled(false);
        chart.setTouchEnabled(true);
        chart.setDragEnabled(true);
        chart.setScaleEnabled(true);
        chart.setPinchZoom(true);
        chart.setBackgroundColor(Color.TRANSPARENT);

        XAxis xAxis = chart.getXAxis();
        xAxis.setTextColor(Color.WHITE);
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);

        chart.getAxisLeft().setTextColor(Color.WHITE);
        chart.getAxisRight().setEnabled(false);
        
        Legend l = chart.getLegend();
        l.setTextColor(Color.WHITE);
        
        chart.setNoDataTextColor(Color.WHITE);
        chart.animateX(1500);
    }

    public static void configurePieChart(PieChart chart) {
        chart.setUsePercentValues(true);
        chart.getDescription().setEnabled(false);
        chart.setExtraOffsets(5, 10, 5, 5);
        chart.setDragDecelerationFrictionCoef(0.95f);
        chart.setDrawHoleEnabled(true);
        chart.setHoleColor(Color.TRANSPARENT);
        chart.setTransparentCircleColor(Color.WHITE);
        chart.setTransparentCircleAlpha(110);
        chart.setHoleRadius(58f);
        chart.setTransparentCircleRadius(61f);
        chart.setDrawCenterText(true);
        chart.setRotationAngle(0);
        chart.setRotationEnabled(true);
        chart.setHighlightPerTapEnabled(true);
        chart.setEntryLabelColor(Color.WHITE);
        
        Legend l = chart.getLegend();
        l.setTextColor(Color.WHITE);
        l.setVerticalAlignment(Legend.LegendVerticalAlignment.TOP);
        l.setHorizontalAlignment(Legend.LegendHorizontalAlignment.RIGHT);
        l.setOrientation(Legend.LegendOrientation.VERTICAL);
        l.setDrawInside(false);
        
        chart.animateY(1400);
    }

    public static void configureBarChart(BarChart chart) {
        chart.getDescription().setEnabled(false);
        chart.setMaxVisibleValueCount(60);
        chart.setPinchZoom(false);
        chart.setDrawGridBackground(false);
        chart.setBackgroundColor(Color.TRANSPARENT);

        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setTextColor(Color.WHITE);

        chart.getAxisLeft().setDrawGridLines(false);
        chart.getAxisLeft().setTextColor(Color.WHITE);
        chart.getAxisRight().setEnabled(false);
        
        Legend l = chart.getLegend();
        l.setTextColor(Color.WHITE);
        
        chart.animateY(1500);
    }
}
