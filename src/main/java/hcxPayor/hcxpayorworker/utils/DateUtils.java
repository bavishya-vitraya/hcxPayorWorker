package hcxPayor.hcxpayorworker.utils;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class DateUtils {

    public static String formatDate(Date date) {
        DateFormat dateFormat = new SimpleDateFormat("MMM d, yyyy h:mm:ss a");
        return dateFormat.format(date);
    }
}
