package mapp.com.audiogravar;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private Button startRecordingButton, stopRecordingButton, startPlaybackButton,
            stopPlaybackButton;
    private TextView statusText;
    private File recordingFile;
    private File somPath;

    public static final String TAG = "ACRCloud";

    public static final int SAMPLING_RATE = 44100;
    public static final int AUDIO_SOURCE = MediaRecorder.AudioSource.MIC;
    public static final int CHANNEL_IN_CONFIG = AudioFormat.CHANNEL_IN_MONO;
    public static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
    public static final int BUFFER_SIZE = AudioRecord.getMinBufferSize(SAMPLING_RATE, CHANNEL_IN_CONFIG, AUDIO_FORMAT);
    public static final String AUDIO_RECORDING_FILE_NAME = "recording.pcm";

    boolean isRecording = false;
    boolean isPlaying = false;

    private RecordAudio recordTask;
    private PlayAudio playTask;

    @Override
    protected void onCreate(Bundle savedInstanceState)  {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Log.v(TAG, "onCreate() " );

        statusText = (TextView) this.findViewById(R.id.StatusTextView);
        startRecordingButton = (Button) this .findViewById(R.id.StartRecordingButton);
        stopRecordingButton = (Button) this .findViewById(R.id.StopRecordingButton);
        startPlaybackButton = (Button) this .findViewById(R.id.StartPlaybackButton);
        stopPlaybackButton = (Button) this.findViewById(R.id.StopPlaybakButton);

        startRecordingButton.setOnClickListener(this);
        stopRecordingButton.setOnClickListener(this);
        startPlaybackButton.setOnClickListener(this);
        stopPlaybackButton.setOnClickListener(this);
        stopRecordingButton.setEnabled(false);
        startPlaybackButton.setEnabled(false);
        stopPlaybackButton.setEnabled(false);


        somPath = new   File(getFilesDir(), "som");
        somPath.mkdirs();

        try {
            recordingFile = File.createTempFile("recording", ".pcm", somPath);
            Log.v(TAG, "recordingFile Create " );
        } catch (IOException e) {
            Log.v(TAG, "IOException " );
        }


    }


    @Override
    public void onClick(View v) {

        if (v == startRecordingButton) {
            record();
            Log.v(TAG, "record()" );
        } else if (v == stopRecordingButton) {
            stopRecording();
            Log.v(TAG, "stopRecording()" );
        } else if (v == startPlaybackButton) {
            play();
            Log.v(TAG, " play()" );
        } else if (v == stopPlaybackButton) {
            stopPlaying();
            Log.v(TAG, "  stopPlaying()" );
        }
    }

    public void play() {
        startPlaybackButton.setEnabled(true);
        playTask = new PlayAudio();
        playTask.execute();
        stopPlaybackButton.setEnabled(true);
    }

    public void stopPlaying() {
        isPlaying = false;
        stopPlaybackButton.setEnabled(false);
        startPlaybackButton.setEnabled(true);
    }

    public void record() {
        startRecordingButton.setEnabled(false);
        stopRecordingButton.setEnabled(true);
// For Fun
        startPlaybackButton.setEnabled(true);
        recordTask = new RecordAudio();
        recordTask.execute();
    }

    public void stopRecording() {
        isRecording = false;
    }


    private class RecordAudio extends AsyncTask<Void, Integer, Void> {

        @Override
        protected Void doInBackground(Void... params) {

            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);
            Log.v(TAG, "Starting recording…");

            isRecording = true;
            byte audioData[] = new byte[BUFFER_SIZE];
            AudioRecord recorder = new AudioRecord(AUDIO_SOURCE,
                    SAMPLING_RATE, CHANNEL_IN_CONFIG,
                    AUDIO_FORMAT, BUFFER_SIZE);


            BufferedOutputStream os = null;
            try {
                os = new BufferedOutputStream(new FileOutputStream(recordingFile.getAbsoluteFile().toString()));
            } catch (FileNotFoundException e) {
                Log.e(TAG, "File not found for recording ", e);
            }

            int r = 0;
            while (isRecording) {
                int status = recorder.read(audioData, 0, audioData.length);

                if (status == AudioRecord.ERROR_INVALID_OPERATION ||
                        status == AudioRecord.ERROR_BAD_VALUE) {
                    Log.e(TAG, "Error reading audio data!");

                }

                try {
                    os.write(audioData, 0, audioData.length);
                } catch (IOException e) {
                    Log.e(TAG, "Error saving recording ", e);

                }
                publishProgress(new Integer(r));
                r++;
            }

            try {
                os.close();

                recorder.stop();
                recorder.release();

                Log.v(TAG, "Recording done…");
                //isRecording = false;

            } catch (IOException e) {
                Log.e(TAG, "Error when releasing", e);
            }


            return null;
        }

        @Override
        protected void onProgressUpdate(Integer... progress) {
            statusText.setText(progress[0].toString());
        }
        @Override
        protected void onPostExecute(Void result) {
            startRecordingButton.setEnabled(true);
            stopRecordingButton.setEnabled(false);
            startPlaybackButton.setEnabled(true);
        }
    }

    private class PlayAudio extends AsyncTask<Void, Integer, Void> {

        @Override
        protected Void doInBackground(Void... params) {

            isPlaying = true;
            Log.v(TAG, "Starting PlayAudio…");
            short[] audiodata = new short[BUFFER_SIZE/4];

            try {

                Log.v(TAG, "Starting PlayAudio 1");
                DataInputStream dis = new DataInputStream(
                        new BufferedInputStream(new FileInputStream(
                                recordingFile)));

                Log.v(TAG, "Starting PlayAudio 2");
                AudioTrack audioTrack = new AudioTrack(
                        AudioManager.STREAM_MUSIC, SAMPLING_RATE,
                        AudioFormat.CHANNEL_CONFIGURATION_MONO, AUDIO_FORMAT, BUFFER_SIZE,
                        AudioTrack.MODE_STREAM);
                Log.v(TAG, "Starting PlayAudio 3");
                audioTrack.play();

                Log.v(TAG, "Starting PlayAudio 4");

                while (isPlaying && dis.available() > 0) {
                    int i = 0;
                    while (dis.available() > 0 && i < audiodata.length) {
                        audiodata[i] = dis.readShort();
                        i++;
                    }
                    audioTrack.write(audiodata, 0, audiodata.length);
                }

                dis.close();

                Log.v(TAG, "Starting PlayAudio 5");

            }catch (Throwable t) {
                Log.v(TAG, "Playback Failed");
            }



            return null;
        }

        @Override
        protected void onPostExecute(Void result) {

            startPlaybackButton.setEnabled(false);
            stopPlaybackButton.setEnabled(true);
        }
    }
}
