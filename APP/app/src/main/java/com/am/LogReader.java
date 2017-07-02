package com.am;

import android.os.Environment;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Created by skaribasappa on 5/29/17.
 */

public class LogReader {
    private static final String TAG = LogReader.class.getCanonicalName();
    private static final String processId = Integer.toString(android.os.Process
            .myPid());

    public static StringBuilder getLog() {

        StringBuilder builder = new StringBuilder();

        try {
            String[] command = new String[] { "logcat", "-d", "-v", "threadtime" };

            Process process = Runtime.getRuntime().exec(command);

            BufferedReader bufferedReader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()));

            String line;
            while ((line = bufferedReader.readLine()) != null) {
                if (line.contains(processId)) {
                    builder.append(line);
                    //Code here
                }
            }
        } catch (IOException ex) {
            Log.e(TAG, "getLog failed", ex);
        }

        return builder;
    }

    public static void writeLog() {
        try {
            File filename = new File(Environment.getExternalStorageDirectory() + "/logbmas.txt");
            filename.createNewFile();
            String cmd = "logcat -v time -f" + filename.getAbsolutePath();
            Runtime.getRuntime().exec(cmd);
        }catch (IOException e) {
            Log.e(TAG, "writeLog failed", e);
        }
    }
}
