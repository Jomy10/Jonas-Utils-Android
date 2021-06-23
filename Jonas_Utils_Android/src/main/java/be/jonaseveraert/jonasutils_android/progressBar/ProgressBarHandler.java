package be.jonaseveraert.jonasutils_android.progressBar;

import android.widget.ProgressBar;

public class ProgressBarHandler implements be.jonaseveraert.util.progressBar.ProgressBarHandler {
    /**
     * The visible element of the progress bar.
     */
    private final ProgressBar pb;

    /**
     * The progressbar will be divided into {@link #numSubProcesses numSubProcesses} equal parts
     */
    private int numSubProcesses = 1;
    /**
     * The amount of activities a subprocess has (e.g. if there are 100 activities in a subprocess,
     * then each activity consists of 1% of the subprocess
     */
    private int[] numActivitiesInSubprocess = new int[numSubProcesses];
    /**
     * The description of the sub process (e.g. Writing file) and the state (e.g. busy or done)
     */
    private Object[] subProcessInfo = new Object[numSubProcesses];
    /**
     * Indicates the percentage the progress bar was already completed
     */
    private double progress = 0;

    private int currentSubProcess = 0;
    private int numActivitiesCompleted = 0;

    /**
     * Creates a new instance of a {@link be.jonaseveraert.util.progressBar.ProgressBarHandler ProgressssBarHandler}
     * that can handle an {@link ProgressBar Android ProgressBar}.
     * @param pb The progressbar
     */
    public ProgressBarHandler(ProgressBar pb) {
        this.pb = pb;
        this.pb.setMax(100);
    }

    /**
     * Sets the amount of sub-processes the process has. e.g. if there are 2 sub-processes, then each of them
     * will take up 50% of the progressbar.
     *
     * @param numSubProcesses The number of sub-processes the process has.
     */
    @Override
    public void setNumSubProcesses(int numSubProcesses) {
        this.numSubProcesses = numSubProcesses;
        this.numActivitiesInSubprocess = new int[this.numSubProcesses];
        this.subProcessInfo = new Object[this.numSubProcesses];
    }

    /**
     * Sets the {@code numActivities} a sub-process with id {@code subProcessID} has.
     * If there are 2 activities in a sub-process, then each activity will take up 50% of the sub-process' part
     * of the progressbar
     *
     * @param subProcessID  The id of the sub-process (in order from when you added the sub-processes, starting at 0)
     * @param numActivities number of activities the sub-process {@code subProcessID} has
     */
    @Override
    public void setNumActivitiesInSubProcesses(int subProcessID, int numActivities) {
        this.numActivitiesInSubprocess[subProcessID] = numActivities;
    }

    /**
     * Will add "..." after the sub-process' name
     */
    public static final int SUBPROCESSINFO_BUSY = 0;
    /**
     * Will add "." after the sub-process' name
     */
    public static final int SUBPROCESSINFO_DONE = 1;
    /**
     * Sets the name and the state of the subprocess
     *
     * @param subProcessID   the id of the subprocess
     * @param subProcessName the subprocess' name (e.g. Writing file)
     * @param state          the subprocess' state (e.g. busy (= 0) or done (= 1). Used to append "..." or "." to the subprocess' name
     */
    @Override
    public void setSubProcessInfo(int subProcessID, String subProcessName, int state) {
        this.subProcessInfo[subProcessID] = new Object[2];
        Object[] subProcessInfoObject = (Object[]) this.subProcessInfo[subProcessID];
        subProcessInfoObject[0] = subProcessName;
        subProcessInfoObject[1] = state;
    }

    /**
     * Sets the name and the state of the busy subprocess.
     *
     * @param subProcessID   the id of the subprocess
     * @param subProcessName the subprocess' name (e.g. Writing file)
     */
    @Override
    public void setSubProcessInfo(int subProcessID, String subProcessName) {
        this.subProcessInfo[subProcessID] = new Object[2];
        Object[] subProcessInfoObject = (Object[]) this.subProcessInfo[subProcessID];
        subProcessInfoObject[0] = subProcessName;
        subProcessInfoObject[1] = SUBPROCESSINFO_BUSY;
    }

    private double percentagePerSubProcess;
    /**
     * Initiates variables and components to start the progress bar and (tries to) show the progress window.
     * Implementation note: This method is used to do things like set the cursor to busy, but most importantly, to calculate
     * what part of the progressbar each sub-process and each activity takes up.
     */
    @Override
    public void startProgressBar() {
        progress = 0;
        pb.setProgress((int) progress);

        // Initial
        Object[] processInfo = (Object[]) subProcessInfo[0];
        // TODO: set text (String) processInfo[0] + (( (int) processInfo[1]) == 0 ? "..." : ".")
        currentSubProcess = 0;
        // Number of activities completed in the current subprocess
        numActivitiesCompleted = 0;

        // Calculate what percentage each sub process takes of the progress bar
        percentagePerSubProcess = 100.0/numSubProcesses;
    }

    /**
     * Completes an activity of the current subprocess
     *
     * @param autoCompleteSubProcess If true, then {@link #completeSubProcess completeSubProcess} will automatically be
     *                               called when all activities of a sub-process have completed.
     */
    @Override
    public void completeActivity(boolean autoCompleteSubProcess) {
        numActivitiesCompleted ++;

        // Update progress bar
        // The percentage of the enitre process the activity takes up
        double percentageOfActivity = percentagePerSubProcess / (numActivitiesInSubprocess[currentSubProcess]);
        updateProgressBar(percentageOfActivity);

        if (numActivitiesCompleted >= numActivitiesInSubprocess[currentSubProcess] && autoCompleteSubProcess) {
            completeSubProcess();
        }
    }

    /**
     * Completes multiple activities, this method cannot autocomplete a sub-process,
     * so the {@link #completeSubProcess() completeSubProcess} has to be called manually
     * (assuming it is not the last sub-process
     *
     * @param numActivitiesCompleted the number of activities the process has to complete
     */
    @Override
    public void completeActivities(int numActivitiesCompleted) {
        this.numActivitiesCompleted += numActivitiesCompleted;

        // Update progress bar
        double percentageOfActivity = (percentagePerSubProcess / (numActivitiesInSubprocess[currentSubProcess])) * numActivitiesCompleted;
        updateProgressBar(percentageOfActivity);
    }

    /**
     * Completes the current subprocess without completing all (or any) activities
     */
    @Override
    public void completeSubProcess() {
        currentSubProcess++;

        if (currentSubProcess != numSubProcesses) {
            numActivitiesCompleted = 0;

            Object[] processInfo = (Object[]) subProcessInfo[currentSubProcess];

            // TODO: set text
            // this.jTextArea.setText((String) processInfo[0] + (((int) processInfo[1]) == 0 ? "..." : "."));

            // Update progress bar
            setProgressBarpercentage(percentagePerSubProcess * (currentSubProcess));
        } else {
            setProgressBarpercentage(100.0);
        }
    }

    /**
     * Adds a percentage to the {@link #progress progress} variable and updates the progressbar that
     * was given in the constructor.
     *
     * @param addPercentage the percentage that needs to be added to the progress bar
     */
    @Override
    public void updateProgressBar(double addPercentage) {
        progress += addPercentage;
        pb.setProgress((int) progress);
    }

    /**
     * Sets the {@link #progress progress} variable and the progressbar to a set percentage.
     *
     * @param percentage the percentage the progressbar will be set to
     */
    @Override
    public void setProgressBarpercentage(double percentage) {
        progress = percentage;
        pb.setProgress((int) progress);
    }

    /**
     * @return the progress of the progressBar
     */
    @Override
    public double getProgress() {
        return pb.getProgress();
    }

    /**
     * This method is called when the process has finished.
     */
    @Override
    public void completeProcess() {
        // Nothing to do here
    }

    /**
     * Sets a message for the loading screen. Can be used to tell the user that the process has finished.
     *
     * @param message the message that will be displayed in the text field
     */
    @Override
    public void setMessage(String message) {
        // TODO
    }

    public ProgressBar getProgressBar() {
        return pb;
    }
}
