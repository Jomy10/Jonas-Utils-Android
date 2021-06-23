package be.jonaseveraert.jonasutils_android.audio;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.os.Build;
import android.os.Process;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.FileAlreadyExistsException;

import be.jonaseveraert.util.progressBar.ProgressBarHandler;

import static android.content.ContentValues.TAG;

public class AudioConverter {

    private final String COMPRESSED_AUDIO_FILE_MIME_TYPE;
    private final int COMPRESSED_AUDIO_FILE_BIT_RATE;
    private final int SAMPLING_RATE;
    private final int BUFFER_SIZE;
    private final int CODEC_TIMEOUT_IN_MS = 5000;
    private final int CHANNELS;
    private be.jonaseveraert.util.progressBar.ProgressBarHandler pbHandler;

    /**
     * Variables for the {@link #COMPRESSED_AUDIO_FILE_MIME_TYPE compressedMimeType} parameter in {@link #AudioConverter AudioConverter}
     */
    public abstract static class MimeType {
        public static final String MP4A_LATM = "audio/mp4a-latm"; // TODO: maybe with enum
    }

    /**
     * Variables for the {@link #CHANNELS amtChannels} parameter in {@link #AudioConverter AudioConverter}
     */
    public abstract static class Channels {
        public static final int STEREO = 2;
        public static final int MONO = 1;
    }

    /**
     *
     * @param compressedMimeType Use {@link MimeType MimeType}.MP4_LATM
     * @param compressedBitRateInBps target bitrate in bits per second
     * @param samplingRate sampling rate
     * @param amtChannels use the {@link Channels Channels} class for the different options
     * @param pbHandler a {@link ProgressBarHandler ProgressBarHandler}. Can be null to not keep track
     *                  of the progress. Has to be set up manually. The {@link #convertAudio convertAudio} subProcess
     *                  has 4 activities.
     */
    public AudioConverter(String compressedMimeType, int compressedBitRateInBps, int samplingRate, int amtChannels, ProgressBarHandler pbHandler) {
        this.COMPRESSED_AUDIO_FILE_MIME_TYPE = compressedMimeType;
        this.COMPRESSED_AUDIO_FILE_BIT_RATE = compressedBitRateInBps; // 64000 = 64 kbps
        this.SAMPLING_RATE = samplingRate;
        this.BUFFER_SIZE = samplingRate; // TODO: ???
        this.CHANNELS = amtChannels;
        this.pbHandler = (be.jonaseveraert.util.progressBar.ProgressBarHandler) pbHandler;
    }

    /**
     * Convets an audio file into another format, specified in the AudioConverter.
     * @param inputFile the file that has to be converted to the specified format.
     * @return true if the compression has finished.
     * @throws FileNotFoundException when the inputFile does not exist
     * @throws FileAlreadyExistsException when the outputFile already exists.
     * @throws IOException if an I/O exception occurs during the creation of the outputFile. Or if
     * an I/O Exception occurs in the initialisation of the {@link MediaMuxer MediaMuxer}. OR if the
     * {@link FileInputStream fis} could not close.
     * @throws AudioFormatNotSupported when the {@link #COMPRESSED_AUDIO_FILE_MIME_TYPE mime type}
     * specified in the constructor is not supported.
     * @implNote request read and write access!
     * @see <a href="https://github.com/tqnst/MP4ParserMergeAudioVideo/blob/master/Mp4ParserSample-master/src/jp/classmethod/sample/mp4parser/MainActivity.java#L335-L442">GitHub</a>
     */
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    public boolean convertAudio(@NonNull File inputFile, @NonNull File outputFile) throws FileNotFoundException, FileAlreadyExistsException, IOException, AudioFormatNotSupported {
        // Input file
        FileInputStream fis = new FileInputStream(inputFile);

        // Output file
        if (outputFile.exists())
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                throw new FileAlreadyExistsException(outputFile.getPath(), null, "File already exists.");
            } else {
                throw new RuntimeException("File already exists: " + outputFile.getPath());
            }
        else {
            if (!(outputFile.createNewFile()))
                throw new FileCouldNotBeCreatedException("The specified outputFile could not be created");
        }

        // Media muxer
        MediaMuxer muxer;
        if (COMPRESSED_AUDIO_FILE_MIME_TYPE.equals(MimeType.MP4A_LATM))
            muxer = new MediaMuxer(outputFile.getAbsolutePath(), MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
        else
            throw new AudioFormatNotSupported("The audio format " + COMPRESSED_AUDIO_FILE_MIME_TYPE + " is not supported.");

        // Output format
        MediaFormat outputFormat = MediaFormat.createAudioFormat(COMPRESSED_AUDIO_FILE_MIME_TYPE, SAMPLING_RATE, CHANNELS);
        if (COMPRESSED_AUDIO_FILE_MIME_TYPE.equals(MimeType.MP4A_LATM)) {
            outputFormat.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);
            outputFormat.setInteger(MediaFormat.KEY_BIT_RATE, COMPRESSED_AUDIO_FILE_BIT_RATE);
            outputFormat.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 16384);
        } else
            throw new AudioFormatNotSupported("The audio format " + COMPRESSED_AUDIO_FILE_MIME_TYPE + " is not supported.");

        // MediaCodec
        MediaCodec codec = MediaCodec.createEncoderByType(COMPRESSED_AUDIO_FILE_MIME_TYPE);
        codec.configure(outputFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        codec.start();

        // Buffer stuff, whatever that means
        ByteBuffer[] codecInputBuffers = codec.getInputBuffers(); // Note: array of buffers
        ByteBuffer[] codecOutputBuffers = codec.getOutputBuffers();

        // So basically the buffer is something temprorary we read the byte data into?
        MediaCodec.BufferInfo outBuffInfo = new MediaCodec.BufferInfo();
        byte[] tempBuffer = new byte[BUFFER_SIZE];
        boolean hasMoreData = true;
        double presentationTimeUs = 0;
        int audioTrackIdx = 0;
        int totalBytesRead = 0;
        int percentComplete = 0;
        int lastPercentageComplete = percentComplete;
        do {
            int inputBufIndex = 0;
            while (inputBufIndex != -1 && hasMoreData) {
                inputBufIndex = codec.dequeueInputBuffer(CODEC_TIMEOUT_IN_MS);

                if (inputBufIndex >= 0) {
                    ByteBuffer dstBuf = codecInputBuffers[inputBufIndex];
                    dstBuf.clear();

                    int bytesRead = -99;
                    try {
                        bytesRead = fis.read(tempBuffer, 0, dstBuf.limit());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    // Log.e("bytesRead", "Readed " + bytesRead);
                    if (bytesRead == -1) { // -1 implies EOS
                        hasMoreData = false;
                        codec.queueInputBuffer(inputBufIndex, 0, 0, (long) presentationTimeUs, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                    } else {
                        totalBytesRead += bytesRead;
                        dstBuf.put(tempBuffer, 0, bytesRead);
                        codec.queueInputBuffer(inputBufIndex, 0, bytesRead, (long) presentationTimeUs, 0);
                        presentationTimeUs = 1000000l * (totalBytesRead / 2) / SAMPLING_RATE;
                    }
                }
            }
            // Drain audio
            int outputBufIndex = 0;
            // I guess these are errors?
            while (outputBufIndex != MediaCodec.INFO_TRY_AGAIN_LATER) {
                outputBufIndex = codec.dequeueOutputBuffer(outBuffInfo, CODEC_TIMEOUT_IN_MS);
                if (outputBufIndex >= 0) {
                    ByteBuffer encodedData = codecOutputBuffers[outputBufIndex];
                    encodedData.position(outBuffInfo.offset);
                    encodedData.limit(outBuffInfo.offset + outBuffInfo.size);
                    if ((outBuffInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0 && outBuffInfo.size != 0) {
                        codec.releaseOutputBuffer(outputBufIndex, false);
                    } else {
                        muxer.writeSampleData(audioTrackIdx, codecOutputBuffers[outputBufIndex], outBuffInfo);
                        codec.releaseOutputBuffer(outputBufIndex, false);
                    }
                } else if (outputBufIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    outputFormat = codec.getOutputFormat();
                    Log.v(TAG, "Output format changed - " + outputFormat);
                    audioTrackIdx = muxer.addTrack(outputFormat);
                    muxer.start();
                } else if (outputBufIndex == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                    //Log.e(TAG, "Output buffers changed during encode!");
                    throw new OutputBuffersChanged("Output buffers changed during encode!");
                } else if (outputBufIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
                    // NO OP
                } else {
                    //Log.e(TAG, "Unkown return code frm dequeueOutputBuffer - " + outputBufIndex);
                    throw new UnkownReturnCode("Unkown return code frm dequeueOutputBuffer - " + outputBufIndex);
                }
            }

            // TODO: get the percentage that is already done in the progressbar and calculate what still needs to be done. -> work with the actual percentages! (or make it variable)
            // TODO: make a background thread for this that sleeps every second and then updates the progressbar
            if (pbHandler != null) {
                percentComplete = (int) Math.round(((float) totalBytesRead / (float) inputFile.length()) * 100.0);
                if (percentComplete >= 25 && lastPercentageComplete < 25) {
                    Thread t = new Thread(completeActivity);
                    t.start();
                    lastPercentageComplete = 25;
                } else if (percentComplete >= 50 && lastPercentageComplete < 50) {
                    Thread t = new Thread(completeActivity);
                    t.start();
                    lastPercentageComplete = 50;
                } else if (percentComplete >= 75 && lastPercentageComplete < 75) {
                    Thread t = new Thread(completeActivity);
                    t.start();
                    lastPercentageComplete = 75;
                } else if (percentComplete >= 95 && lastPercentageComplete < 95) {
                    Thread t = new Thread(completeActivity);
                    t.start();
                    lastPercentageComplete = 100;
                }
            }
            //Log.v(TAG, "Conversion % - " + percentComplete);
        } while (outBuffInfo.flags != MediaCodec.BUFFER_FLAG_END_OF_STREAM);

        fis.close();
        muxer.stop();
        muxer.release();

        return true;
    }

    private final Runnable completeActivity = new Runnable() {
        @Override
        public void run() {
            Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
            pbHandler.completeActivity(true);
        }
    };
}
