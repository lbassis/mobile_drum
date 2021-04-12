package com.example.sequenciador;

import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import androidx.core.app.ActivityCompat;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.Toast;

import com.leff.midi.MidiFile;
import com.leff.midi.MidiTrack;
import com.leff.midi.event.ChannelEvent;
import com.leff.midi.event.MidiEvent;
import com.leff.midi.event.ProgramChange;
import com.leff.midi.event.ProgramChange.MidiProgram;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import linc.com.amplituda.Amplituda;

import static com.leff.midi.event.ChannelEvent.PROGRAM_CHANGE;
import static com.leff.midi.event.ProgramChange.MidiProgram.AGOGO;
import static com.leff.midi.event.ProgramChange.MidiProgram.APPLAUSE;
import static com.leff.midi.event.ProgramChange.MidiProgram.MELODIC_TOM;
import static com.leff.midi.event.ProgramChange.MidiProgram.REVERSE_CYMBAL;
import static com.leff.midi.event.ProgramChange.MidiProgram.STEEL_DRUMS;
import static com.leff.midi.event.ProgramChange.MidiProgram.SYNTH_DRUM;
import static com.leff.midi.event.ProgramChange.MidiProgram.TAIKO_DRUM;

public class MainActivity extends AppCompatActivity {
    private Button play, record;
    private SeekBar seek_bar;
    private MediaRecorder myAudioRecorder;
    private String outputFile;
    private Handler handler;

    class ProgressThread extends Thread {
        int position, sum, count;
        String mode;
        ProgressThread(String mode) {
            this.position = 0;
            this.mode = mode;
            this.sum = 0;
            this.count = 0;
        }

        public void run() {
            while (position < 50) {
                seek_bar.setProgress(position);
                position++;
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            if (mode == "recording") {
                myAudioRecorder.stop();
                myAudioRecorder.release();
                myAudioRecorder = null;
            }



            MidiTrack noteTrack = new MidiTrack();
            //noteTrack.insertEvent(inst);
            Amplituda amplituda = new Amplituda(getApplicationContext());
            amplituda.fromPath(outputFile)
                    .amplitudesAsList(list -> {
                        for(int tmp : list) {
                            count++;
                            if (tmp > 70) {
                                noteTrack.insertNote(9, 33, 100, count*128, 128);
                            }
                            //else {
                            //    noteTrack.insertNote(0, 0, 100, count*128, 128);
                            //}
                        }

                    });

            ArrayList<MidiTrack> tracks = new ArrayList<MidiTrack>();
            tracks.add(noteTrack);
            MidiFile midi = new MidiFile(MidiFile.DEFAULT_RESOLUTION, tracks);

            File output = new File(getExternalCacheDir().getAbsolutePath()+"/exported.mid");
            try {
                midi.writeToFile(output);
            }
            catch(IOException e) {
                System.err.println(e);
            }


            runOnUiThread(new Runnable() {

                @Override
                public void run() {

                    if (mode == "recording") {
                        record.setEnabled(true);
                        play.setEnabled(true);
                        Toast.makeText(getApplicationContext(), "Audio recorded successfully", Toast.LENGTH_LONG).show();
                    }
                    else if (mode == "playing") {
                    }
                }
            });
            }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        play = (Button) findViewById(R.id.play);
        record = (Button) findViewById(R.id.record);
        seek_bar = findViewById(R.id.seekBar);
        play.setEnabled(false);


        //outputFile = Environment.getExternalStorageDirectory().getAbsolutePath() + "/recording.3gp";
        outputFile = getExternalCacheDir().getAbsolutePath();
        outputFile += "/audiorecordtest.3gp";
        System.out.println(outputFile);

        record.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                myAudioRecorder = new MediaRecorder();
                myAudioRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
                myAudioRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
                myAudioRecorder.setAudioEncoder(MediaRecorder.OutputFormat.AMR_NB);
                myAudioRecorder.setOutputFile(outputFile);

                try {
                    myAudioRecorder.prepare();
                    myAudioRecorder.start();

                } catch (IllegalStateException ise) {
                    Toast.makeText(getApplicationContext(), "illegal state", Toast.LENGTH_LONG).show();
                    // make something ...
                } catch (IOException ioe) {
                    Toast.makeText(getApplicationContext(), "io exception", Toast.LENGTH_LONG).show();

                    // make something
                }
                record.setEnabled(false);
                Toast.makeText(getApplicationContext(), "Recording started", Toast.LENGTH_LONG).show();
                ProgressThread p = new ProgressThread("recording");
                p.start();
            }
        });

        play.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MediaPlayer mediaPlayer = new MediaPlayer();
                try {
                    mediaPlayer.setDataSource(getExternalCacheDir().getAbsolutePath()+"/exported.mid");
                    mediaPlayer.prepare();
                    mediaPlayer.start();
                    Toast.makeText(getApplicationContext(), "Playing Audio", Toast.LENGTH_LONG).show();
                    ProgressThread p = new ProgressThread("playing");
                    p.start();
                } catch (Exception e) {
                    // make something
                }
            }
        });
    }
}