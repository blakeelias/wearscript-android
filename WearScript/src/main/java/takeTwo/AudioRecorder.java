package takeTwo;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import java.io.File;

public class AudioRecorder extends Service {
	
	private final String LOG_TAG = "AudioRecorder";
    private final IBinder mBinder = new LocalBinder();

    public AudioRecordThread recorder;
    public static String MILLIS_EXTRA_KEY = "millis";

	@Override
    public void onCreate() {
        super.onCreate();

        Log.d(LOG_TAG, "Service Started");
        createDirectory();
    }

    @Override
    public void onDestroy() {
    	Log.d(LOG_TAG, "Service Destroy");
    	recorder.interrupt();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }
	
	private void createDirectory() {
        File directory = new File(AudioRecordThread.directoryAudio);
        if (!directory.isDirectory()){
        	directory.mkdirs();
        }
	}

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent.getAction() != null) {
            if (intent.getAction().equals("com.wearscript.record.RECORD_AUDIO")) {
                startRecording(intent.getStringExtra("filePath"));
            } else if (intent.getAction().equals("com.wearscript.record.SAVE_AUDIO")) {
                saveFile();
            } else if (intent.getAction().equals("com.wearscript.record.STOP_AUDIO")) {
                stopRecording();
            }
        }
        return 0;
    }

    public void startRecording(String filePath) {
        recorder = new AudioRecordThread(this);
        recorder.start();
    }

    public void saveFile() {
        recorder.writeAudioDataToFile();
    }

    public void saveAndStartNewFile(String filePath) {
        recorder.writeAudioDataToFile(filePath);
    }

    public void stopRecording() {
        recorder.stopRecording();
        stopSelf();
    }

    public class LocalBinder extends Binder {
        public AudioRecorder getService() {
            // Return this instance of AudioRecorder so clients can call public methods
            return AudioRecorder.this;
        }
    }
}
