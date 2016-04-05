package com.example.timalka.vocoder;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    private AudioRecord audioRecorder;
    private int audioSource = MediaRecorder.AudioSource.MIC;
    private int sampleRateInHz = 44100;
    private int channelConfig = AudioFormat.CHANNEL_IN_MONO;
    private int audioFormat = AudioFormat.ENCODING_PCM_16BIT;
    private int bufferSizeInBytes = AudioRecord.getMinBufferSize(44100,AudioFormat.CHANNEL_IN_MONO,AudioFormat.ENCODING_PCM_16BIT);

    private static final String tempfilename = "temp.raw";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    private void recordButtonClicked(View view){

        audioRecorder = new AudioRecord(audioSource,sampleRateInHz, channelConfig,audioFormat,bufferSizeInBytes);
        audioRecorder.startRecording();
        readDatatoTemporaryFile();

    }

    private String theTemporaryFile(){

        String filepath = Environment.getExternalStorageDirectory().getPath();
        //main folder
        File file = new File(filepath,"Recordings");

        if (!file.exists()){

            file.mkdirs();
        }

        //temporary file
        File tempFile = new File(filepath,tempfilename);

        if (tempFile.exists()){

            tempFile.delete();
        }

        return (file.getAbsolutePath() + "/" + tempfilename);

    }

    private String theSavedFile(){

        String filepath = Environment.getExternalStorageDirectory().getPath();
        //main folder
        File file = new File(filepath,"Recordings");

        if (!file.exists()){

            file.mkdirs();
        }

        return (file.getAbsolutePath() + "/" + System.currentTimeMillis() + ".wav");

    }

    private void readDatatoTemporaryFile(){

        byte recording[] = new  byte[bufferSizeInBytes];
        String fileName = theTemporaryFile();
        FileOutputStream fileOutputStream = null;

        try {
            fileOutputStream = new FileOutputStream(fileName);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }


        try {
            if (fileOutputStream != null) {
                //read data to byte array
                audioRecorder.read(recording,0,bufferSizeInBytes);
                // read array to temporary file
                fileOutputStream.write(recording);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void stopRecordButtonClicked(View view){

        audioRecorder.stop();
        audioRecorder.release();
    }


    private void saveRecordingButtonClicked (View view){
        // save recording to wav.file
        converttoWav(theTemporaryFile(),theSavedFile());

        // delete temporary file
        File file = new File(theTemporaryFile());
        file.delete();

        // show toast when recording is saved
        String saved = "The recording has been saved";
        Toast.makeText(this,saved,Toast.LENGTH_SHORT).show();
    }

    private void converttoWav(String fileIn, String fileOut){

        long totalAudioLen = 0; // use with file in
        long totalDataLen = totalAudioLen + 36;
        long sampleRate = 44100;
        int channels = 1;
        long byteRate = 44100 * 16 * 1/8;

        byte[] therecording = new byte[bufferSizeInBytes];

        try{

            FileInputStream readfromfile = new FileInputStream(fileIn);
            FileOutputStream writetofile = new FileOutputStream(fileOut);

            totalAudioLen = readfromfile.getChannel().size();
            totalDataLen = totalAudioLen + 36;
            wavHeader(writetofile,totalAudioLen,totalDataLen,sampleRate,channels,byteRate);

            while (readfromfile.read(therecording) != -1){

                writetofile.write(therecording);
            }

            readfromfile.close();
            writetofile.close();


        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // provides specifications about the raw data
    private void wavHeader(FileOutputStream out,long totalAudioLen,
                           long totalDataLen, long sampleRate, int channels, long byteRate) throws IOException {

    // & is a bitwise AND operator - multiplies two equal length binary representations
    // >> e.g x = y >> 2  ... x is the result of y shifting to the right by 2 bits

    // a wav file is 44 bits long
        byte[] header = new byte[44];

        //Marking the file as a riff
        header[0]= 'R';
        header[1]= 'I';
        header[2]= 'F';
        header[3]= 'F';
        //Overall file size
        //shift to the first bit and convert (masks) to 8 bits
        header[4]= (byte) (totalDataLen & 0xFF);
        //shift to the 8th bit and convert (masks) to 8 bits
        header[5]= (byte) ((totalDataLen >> 8 ) & 0xFF);
        //shift to the 16th bit and convert (masks) to 8 bits
        header[6]= (byte) ((totalDataLen >> 16) & 0xFF);
        //shift to the 24th bit and convert (masks) to 8 bits
        header[7]= (byte) ((totalDataLen >> 24) & 0xFF);
        //WAVE
        header[8]= 'W';
        header[9]= 'A';
        header[10]= 'V';
        header[11]= 'E';
        //format
        header[12]= 'f';
        header[13]= 'm';
        header[14]= 't';
        //length of format data
        header[15]= ' ';
        header[16]= 16;
        //type of format
        header[17]= 0;
        header[18]= 0 ;
        header[19]= 0;
        header[20]= 1;
        //number of channels
        header[21]= 0;
        header[22]= (byte) channels;
        header[23]= 0;
        //sample rate
        header[24]= (byte)(sampleRate & 0xFF);
        header[25]= (byte)((sampleRate >> 8) & 0xFF);
        header[26]= (byte)((sampleRate >> 16) & 0xFF);
        header[27]= (byte)((sampleRate >> 24) & 0xFF);
        //byte rate (Sample Rate * BitsPerSample * Channels) / 8
        header[28]= (byte)(byteRate & 0xFF);
        header[29]= (byte)((byteRate >> 8) & 0xFF);
        header[30]= (byte)((byteRate >> 16) & 0xFF);
        header[31]= (byte)((byteRate >> 24) & 0xFF);;
        header[32]= (byte) (1*16/8);
        header[33] = 0;
        // bits per sample
        header[34] = 16;
        header[35]= 0;
        //data section
        header[36]= 'd';
        header[37]= 'a';
        header[38]= 't';
        header[39]= 'a';
        //size of data
        header[40]= (byte)(totalAudioLen & 0xFF);
        header[41]= (byte) ((totalAudioLen >> 8) & 0xFF);
        header[42]= (byte)((totalAudioLen >> 16) & 0xFF);
        header[43]= (byte) ((totalAudioLen >> 24) & 0xFF);

        out.write(header,0,44);
    }
}
