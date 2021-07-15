/*
 * Copyright (c) Australian Institute of Marine Science, 2021.
 * @author Gael Lafond <g.lafond@aims.gov.au>
 */
package au.gov.aims.ncanimate;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public abstract class SystemCallThread extends Thread {
    // Bash Exit codes:
    //   http://tldp.org/LDP/abs/html/exitcodes.html
    public static final int SUCCESS = 0;
    public static final int ERROR = 1;
    public static final int COMMAND_NOT_FOUND = 127;

    // 128+n -> Fatal error signal "n"
    // For signal number: man kill
    public static final int INTERRUPTED = 128 + 2;
    public static final int KILLED = 128 + 9;
    public static final int SEGMENTATION_FAULT = 128 + 11;
    public static final int BROKEN_PIPE = 128 + 13;
    public static final int TERMINATED = 128 + 15;


    private boolean running;
    private Integer exitCode;

    private String commandLine;
    private Process process;
    private Map<String, String> environmentVariables;

    private boolean succeed;

    public SystemCallThread(String commandLine) {
        this.exitCode = null;
        this.running = false;
        this.process = null;

        this.commandLine = commandLine;

        this.environmentVariables = new HashMap<String, String>();
    }

    // Notify when the script stop running.
    public abstract void onStart();

    // Notify when something goes wrong.
    public abstract void onException(Exception ex);

    // Notify when the script stop running.
    public abstract void onStop(boolean succeed);

    // Can be overwritten
    public void stdout(String logLine) {
        System.out.println(logLine);
    }

    // Can be overwritten
    public void stderr(String errorLine) {
        System.err.println(errorLine);
    }


    public void setEnvironmentVariable(String key, String value) {
        this.environmentVariables.put(key, value);
    }

    /**
     * Semaphore
     * Set the "running" flag.
     * Return true if the flag has been set. False if the flag was already set.
     * This is used to ensure the script can't be run twice, even if the requests
     * are extremely close to each other (racing condition).
     */
    private synchronized boolean setRunning(boolean running) {
        if (running && this.running) {
            // It's already running
            return false;
        }
        if (!running && !this.running) {
            // It's already stopped
            return false;
        }
        this.running = running;
        return true;
    }

    public synchronized boolean isRunning() {
        return this.isAlive() && this.running;
    }


    private synchronized void setExitCode(Integer exitCode) {
        this.exitCode = exitCode;
    }

    public synchronized Integer getExitCode() {
        return this.exitCode;
    }


    public String getCommandLine() {
        return this.commandLine;
    }


    @Override
    public void run() {
        // NOTE: setRunning returns true if and only if we try to change it's value.
        //   I.E. Calling setRunning(true) when the script is already running will return false.
        //   This is used to avoid concurrent execution of the script (racing condition).
        if (this.commandLine != null && this.setRunning(true)) {
            StreamCollector stderr = null;
            StreamCollector stdout = null;

            try {
                String[] scriptCommand = SystemCallThread.parseCommandLine(this.commandLine);

                // Create a ProcessBuilder using the command + parameters
                ProcessBuilder processBuilder = new ProcessBuilder(scriptCommand);

                if (!this.environmentVariables.isEmpty()) {
                    Map<String, String> env = processBuilder.environment();
                    for (Map.Entry<String, String> environmentVariable : this.environmentVariables.entrySet()) {
                        env.put(environmentVariable.getKey(), environmentVariable.getValue());
                    }
                }

                // Execute the command
                this.succeed = true;
                this.process = processBuilder.start();

                // Grab std error (otherwise it will hang as soon as the buffer is full)
                stderr = new StreamCollector(this.process.getErrorStream()) {
                    @Override
                    public void onLog(String logLine) {
                        SystemCallThread.this.succeed = false;
                        // NOTE: A line of log on the error stream is an error
                        SystemCallThread.this.stderr(logLine);
                    }

                    @Override
                    public void onException(String errorLine) {
                        SystemCallThread.this.succeed = false;
                        SystemCallThread.this.stderr(errorLine);
                    }
                };
                stderr.start();

                // Grab std out (otherwise it will hang as soon as the buffer is full)
                stdout = new StreamCollector(this.process.getInputStream()) {
                    @Override
                    public void onLog(String logLine) {
                        SystemCallThread.this.stdout(logLine);
                    }

                    @Override
                    public void onException(String errorLine) {
                        SystemCallThread.this.succeed = false;
                        SystemCallThread.this.stderr(errorLine);
                    }
                };
                stdout.start();

                this.onStart();

                // Wait for the command to terminate
                // NOTE: This is running in a separate thread so it doesn't block the main thread.
                // Exit codes:
                //   http://tldp.org/LDP/abs/html/exitcodes.html
                this.setExitCode(this.process.waitFor());
                stdout.join();
                stderr.join();

            } catch (IOException ex) {
                this.succeed = false;
                // Something went wrong (script file not found, not executable, not readable, etc)
                this.setExitCode(SystemCallThread.ERROR);
                this.onException(ex);
            } catch (InterruptedException ex) {
                this.succeed = false;
                // The execution was cancelled (someone hit the "Stop" button)
                if (this.process != null) {
                    this.process.destroy();
                    this.process = null;
                }
                this.setExitCode(SystemCallThread.INTERRUPTED);
            } finally {
                if (stderr != null && stderr.isAlive()) {
                    stderr.interrupt();
                }
                if (stdout != null && stdout.isAlive()) {
                    stdout.interrupt();
                }

                // The script terminated (nicely or not)
                this.setRunning(false);
                // Notify the listeners about the termination of the script
                this.onStop(!this.succeed || SystemCallThread.SUCCESS != this.getExitCode());
            }
        }
    }

    protected static String[] parseCommandLine(String commandLine) {
        // Loop through each character. Add them to current "argument"
        // If character is a double quote (and current "argument" is empty), inQuote = true
        // If character is a double quote (not preceded by backslash) and inQuote is true, inQuote = false, end of "argument".

        ArrayList<String> argumentList = new ArrayList<String>();

        StringBuilder argument = new StringBuilder();
        boolean quoted = false;
        int backslashCount = 0;

        boolean add, end;
        for (char car : commandLine.toCharArray()) {
            add = true; end = false;
            if (car == '"') {
                if (quoted) {
                    if (backslashCount % 2 == 0) {
                        quoted = false;
                        end = true;
                        add = false;
                    }
                } else {
                    if (argument.length() == 0) {
                        quoted = true;
                        add = false;
                    }
                }
            } else if (car == ' ') {
                if (!quoted) {
                    end = true;
                    add = false;
                }
            }

            if (car == '\\') {
                backslashCount++;
            } else {
                backslashCount = 0;
            }

            if (add) {
                argument.append(car);
            }

            if (end) {
                String argumentStr = argument.toString();
                if (!argumentStr.isEmpty()) {
                    // Remove extraneous backslashes
                    argumentStr = argumentStr
                        .replace("\\\"", "\"")  // Replace \" with "
                        .replace("\\\\", "\\"); // Replace \\ with \

                    argumentList.add(argumentStr);
                }
                argument = new StringBuilder();
            }
        }

        if (argument.length() > 0) {
            argumentList.add(argument.toString());
        }

        return argumentList.toArray(new String[argumentList.size()]);
    }

    private static JSONArray exceptionToJSON(Throwable exception) {
        if (exception == null) {
            return null;
        }

        JSONArray jsonExceptions = new JSONArray();

        Throwable cause = exception;
        while (cause != null) {
            JSONObject jsonException = new JSONObject();

            String exceptionMessage = cause.getMessage();
            if (exceptionMessage == null) {
                jsonException.put("message", "Unexpected error occurred: " + exception.getClass().getSimpleName());
            } else {
                jsonException.put("message", exceptionMessage);
            }

            JSONArray causeStackTrace = new JSONArray();
            for (StackTraceElement element : cause.getStackTrace()) {
                if (element != null) {
                    causeStackTrace.put(element.toString());
                }
            }
            if (causeStackTrace.length() > 0) {
                jsonException.put("stacktrace", causeStackTrace);
            }

            jsonExceptions.put(jsonException);

            cause = cause.getCause();
        }

        return jsonExceptions;
    }

    /**
     * Stream collector.
     * Used to collect data going through a process output byte stream
     * (stdout and stderr).
     *
     * If the data is not collected, the buffer will eventually get full
     * and Java will wait for the buffer to be empty before collecting
     * more data, freezing the whole process.
     *
     * The class MUST be used with output stream (stdout) and
     * error stream (stderr) when executing an external process.
     *
     * If the stream can be ignored, simple use the StreamCollector
     * with empty `onLog()` and `onException()` methods.
     * Do NOT dismiss the process output stream.
     *
     * Inspired on: https://thilosdevblog.wordpress.com/2011/11/21/proper-handling-of-the-processbuilder/
     */
    public abstract static class StreamCollector extends Thread {
        private InputStream in;

        public StreamCollector(InputStream in) {
            this.in = in;
        }

        public abstract void onLog(String logLine);
        public abstract void onException(String errorLine);

        @Override
        public void run() {
            BufferedReader reader = null;
            try {
                reader = new BufferedReader(new InputStreamReader(this.in));
                String line;
                while ( (line = reader.readLine()) != null) {
                    this.onLog(line);
                }
            } catch (Exception e) {
                this.addException(e);
            } finally {
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (IOException e) {
                        this.addException(e);
                    }
                }
            }
        }

        private void addException(Exception ex) {
            JSONArray jsonExceptionArray = SystemCallThread.exceptionToJSON(ex);
            if (jsonExceptionArray != null) {
                for (Object rawJsonExceptionLine : jsonExceptionArray) {
                    this.onException(rawJsonExceptionLine.toString());
                }
            }
        }
    }
}
