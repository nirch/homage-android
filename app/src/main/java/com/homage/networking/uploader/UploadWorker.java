package com.homage.networking.uploader;

import java.util.Dictionary;

public interface UploadWorker {
    /**
     *  Sets the source url to read the file from and the destination url to upload the file to.
     *
     *  @param jobID          a unique string identifier for the new job.
     *  @param source      string value representing the source of the file, usually on the local file system.
     *  @param destination string value representing the destination url to upload to, usually on some remote server.
     *
     *   Remark - The implementation will decide what source and destination actually means (can be urls or anything else).
     */
    public void newJob(String jobID, String source, String destination);

    /**
     *  Gives the worker the command to start uploading the file.
     *
     *   @return true if was able to start working. false otherwise. (A manager probably should check this returned value)
     *
     */
    public boolean startWorking();

    /**
     *  Tells a working worker to stop / cancel what it is currently doing.
     */
    public void stopWorking();

    /**
     * Gets the worker's set job id.
     * @return String of the set job id (or null)
     */
    public String getJobID();

    /**
     * Gets the worker's set source file path.
     *
     * @return String of the set source file path (or null).
     */
    public String getSource();

    public String getNewSource();

    /**
     * resets worker to initial idle state.
     */
    public void reset();

    /**
     * Reports that the source file changed during the upload.
     */
    public void reportSourceFileChangedDuringUpload(String newSource);
}
