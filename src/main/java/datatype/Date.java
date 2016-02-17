package datatype;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;

public class Date {

    protected int day;
    protected int month;
    protected int year;

    private Calendar c;

    public Date() {
        this.c = new GregorianCalendar();
        c.setTimeZone(TimeZone.getTimeZone("GMT"));
    }

    public Date(int month, int day, int year) {
        this.month = month;
        this.day = day;
        this.year = year;
        this.c = new GregorianCalendar();
        c.setTimeZone(TimeZone.getTimeZone("GMT"));
    }

    public void reset(int month, int day, int year) {
        this.month = month;
        this.day = day;
        this.year = year;
    }

    public int getDay() {
        return day;
    }

    public int getMonth() {
        return month;
    }

    public int getYear() {
        return year;
    }

    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("date");
        builder.append("(\"");
        builder.append(year);
        builder.append("-");
        builder.append(month < 10 ? "0" + month : "" + month);
        builder.append("-");
        builder.append(day < 10 ? "0" + day : "" + day);
        builder.append("\")");
        return builder.toString();
    }

    public void setDay(int day) {
        this.day = day;
    }

    public void setMonth(int month) {
        this.month = month;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public long toTimestamp() {
        c.set(year, (month - 1), day); //In the Gregorian Calendar, first month is 0
        return c.getTimeInMillis();
    }

    public void reset(long ts) {
        c.setTimeInMillis(ts);
        this.year = c.get(Calendar.YEAR);
        this.month = c.get(Calendar.MONTH) + 1;
        this.day = c.get(Calendar.DAY_OF_MONTH);
    }

    public void reset(Date d) {
        this.year = d.getYear();
        this.month = d.getMonth();
        this.day = d.getDay();
        c.set(year, (month - 1), day);
    }
}