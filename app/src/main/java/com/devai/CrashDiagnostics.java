package com.devai;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public final class CrashDiagnostics {
    private static volatile boolean installed;

    private CrashDiagnostics() { }

    public static synchronized void install(Context context) {
        if (installed) return;
        installed = true;
        final Context app = context.getApplicationContext();
        final Thread.UncaughtExceptionHandler previous = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler((thread, error) -> {
            try {
                StringWriter trace = new StringWriter();
                error.printStackTrace(new PrintWriter(trace));
                String report = "Dev Orus 1.1 Java crash\n"
                        + "Time: " + new Date() + "\n"
                        + "Thread: " + thread.getName() + "\n\n"
                        + trace + "\n\nLast recorded stages:\n"
                        + read(new File(app.getFilesDir(), "devorus-diagnostics.txt"));
                write(new File(app.getFilesDir(), "devorus-crash.txt"), report, false);
            } catch (Throwable ignored) { }
            if (previous != null) previous.uncaughtException(thread, error);
        });
    }

    public static synchronized void record(Context context, String stage) {
        try {
            String stamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)
                    .format(new Date());
            write(new File(context.getApplicationContext().getFilesDir(),
                    "devorus-diagnostics.txt"), stamp + " | " + stage + "\n", true);
        } catch (Throwable ignored) { }
    }

    private static void write(File file, String value, boolean append) throws Exception {
        try (FileOutputStream out = new FileOutputStream(file, append)) {
            out.write(value.getBytes(StandardCharsets.UTF_8));
            out.flush();
            out.getFD().sync();
        }
    }

    private static String read(File file) {
        try {
            if (!file.isFile()) return "(No previous diagnostic stages.)";
            byte[] data = new byte[(int) Math.min(file.length(), 256 * 1024)];
            try (java.io.FileInputStream in = new java.io.FileInputStream(file)) {
                int count = in.read(data);
                return count > 0 ? new String(data, 0, count, StandardCharsets.UTF_8) : "";
            }
        } catch (Exception error) {
            return "Could not read diagnostics: " + error;
        }
    }

    public static boolean hasReport(Context context) {
        File dir = context.getApplicationContext().getFilesDir();
        return new File(dir, "devorus-crash.txt").isFile()
                || new File(dir, "devorus-diagnostics.txt").isFile();
    }

    public static void show(Activity activity) {
        File dir = activity.getFilesDir();
        StringBuilder report = new StringBuilder("Dev Orus 1.1 diagnostic report\n\n");
        File crash = new File(dir, "devorus-crash.txt");
        File stages = new File(dir, "devorus-diagnostics.txt");
        if (crash.isFile()) report.append("JAVA CRASH REPORT\n").append(read(crash)).append("\n\n");
        report.append("RECENT EXECUTION STAGES\n").append(read(stages));
        String content = report.toString();
        new AlertDialog.Builder(activity)
                .setTitle("Crash report")
                .setMessage(content)
                .setNegativeButton("Close", null)
                .setPositiveButton("Share", (dialog, which) -> {
                    Intent share = new Intent(Intent.ACTION_SEND);
                    share.setType("text/plain");
                    share.putExtra(Intent.EXTRA_SUBJECT, "Dev Orus 1.1 crash diagnostics");
                    share.putExtra(Intent.EXTRA_TEXT, content);
                    activity.startActivity(Intent.createChooser(share, "Share crash report"));
                }).show();
    }
}
