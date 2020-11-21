package net.wurmunlimited.forge.interfaces;

import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public interface JsonStringBuilder {

    DateFormat DATE_FORMAT = new SimpleDateFormat("yyyyMMdd",Locale.ENGLISH);

    String INDENT = "  ";

    static String toJsonString(String str) {
        return str!=null? "\""+str+"\"" : "null";
    }

    static String toJsonString(Date date) {
        return date!=null? "\""+DATE_FORMAT.format(date)+"\"" : "null";
    }

    static String getJsonString(JsonObject jo,String name,String defaultValue) {
        JsonValue value = jo.get(name);
        return value!=null && !value.isNull()? value.asString() : defaultValue;
    }

    void toJsonString(StringBuilder sb,String ind);
}
