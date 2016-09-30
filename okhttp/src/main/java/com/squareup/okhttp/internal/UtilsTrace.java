package com.squareup.okhttp.internal;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.text.NumberFormat;
import java.util.Locale;

public class UtilsTrace {

	public static String toStackTrace(Exception e) {
		if(e == null) return "";
	    final Writer result = new StringWriter();
	    final PrintWriter printWriter = new PrintWriter(result);
	    e.printStackTrace(printWriter);
	    return result.toString();
	}

	public static String printStackTrace() {
		return printStackTrace(Thread.currentThread().getStackTrace());
	}

	public static String printStackTrace(Exception e) {
		if(e == null) return "";
		return printStackTrace(e.getStackTrace());
	}

	private static String printStackTrace(StackTraceElement[] elements) {
		if(elements == null) return "";

		final StringBuilder sb = new StringBuilder();

		boolean add = false;
		for (StackTraceElement e : elements) {
			if (e == null) {
				continue;
			}

			if (!add && "printStackTrace".equals(e.getMethodName())) {
				add = true;
			} else if (add) {
				sb.append(e.getClassName()).append(' ').append(e.getMethodName()).append(':').append(e.getLineNumber()).append('\n');
			}
		}

		for(StackTraceElement e : elements) {
			if(e == null) continue;
			sb.append(e.getClassName()).append(' ').append(e.getMethodName()).append(':').append(e.getLineNumber()).append('\n');
		}
		return sb.toString();
	}

	public static String doubleToString(double val) {
	    NumberFormat nf = NumberFormat.getInstance(Locale.US);
	    nf.setGroupingUsed(false);
	    nf.setMaximumFractionDigits(6);
	    return nf.format(val);
	}

}
