package com.example.sequenciador;

import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import androidx.core.app.ActivityCompat;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Handler;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.Spinner;
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
    private ImageButton record0, record1;
    private Button play;
    private SeekBar seek_bar0, seek_bar1;
    private MediaRecorder myAudioRecorder;
    private String outputFile;
    private Handler handler;
    MidiTrack noteTrack;
    MediaPlayer mediaPlayer;


    class ProgressThread extends Thread {
        int position, sum, count, last_value;
        boolean going_up;
        String mode;
        SeekBar seek_bar, seek_bar2;

        ProgressThread(String mode, SeekBar seek_bar) {
            this.position = 0;
            this.mode = mode;
            this.sum = 0;
            this.count = 0;
            this.last_value = -1;
            this.going_up = false;
            this.seek_bar = seek_bar;
            this.seek_bar2 = null;
        }

        ProgressThread(String mode, SeekBar seek_bar, SeekBar seek_bar2) {
            this.position = 0;
            this.mode = mode;
            this.sum = 0;
            this.count = 0;
            this.last_value = -1;
            this.going_up = false;
            this.seek_bar = seek_bar;
            this.seek_bar2 = seek_bar2;
        }

        public void create_midi() {
            Amplituda amplituda = new Amplituda(getApplicationContext());
            amplituda.fromPath(outputFile)
                    .amplitudesAsList(list -> {
                        for(int tmp : list) {
                            count++;
                            if (tmp > 13) {
                                //System.out.println(tmp + " " + last_value);
                                //if (tmp < last_value) {
                                    going_up = false;
                                    System.out.println("som no momento" + count);
                                    noteTrack.insertNote(9, 44, 100, (count - 1) * 128, 128);
                                //}
                            }

                            else {
                                noteTrack.insertNote(9, 0, 100, (count-1)*128, 128);
                            }

                            if (tmp > last_value) {
                                going_up = true;
                            }
                            else {
                                going_up = false;
                            }

                            last_value = tmp;

                            System.out.println(tmp);
                        }

                    });

        }
        public void run() {

            if (mode == "playing") {
                while (true) {
                    position = 0;
                    mediaPlayer.start();
                    while (position < 50) {
                        seek_bar.setProgress(position);
                        if (seek_bar2 != null) {
                            seek_bar2.setProgress(position);
                        }
                        position++;
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    mediaPlayer.stop();
                    try {
                        mediaPlayer.prepare();
                    }
                    catch (IOException e){}
                        //mediaPlayer.prepare();
                }
            }
            while (position < 50) {
                seek_bar.setProgress(position);
                if (seek_bar2 != null) {
                    seek_bar2.setProgress(position);
                }
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

            create_midi();

            runOnUiThread(new Runnable() {

                @Override
                public void run() {

                    if (mode == "recording") {
                        record0.setEnabled(true);
                        record1.setEnabled(true);
                        play.setEnabled(true);
                        Toast.makeText(getApplicationContext(), "Audio recorded successfully", Toast.LENGTH_LONG).show();
                    }
                    else if (mode == "playing") {
                        record0.setEnabled(true);
                        record1.setEnabled(true);
                        play.setEnabled(true);
                    }
                }
            });
            }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //get the spinner from the xml.
        String[] spinner_items = new String[]{"Pedal Hi-hat"};

        Spinner dropdown0 = findViewById(R.id.spinner0);
        Spinner dropdown1 = findViewById(R.id.spinner1);

        ArrayAdapter<String> adapter0 = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, spinner_items);
        ArrayAdapter<String> adapter1 = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, spinner_items);

        dropdown0.setAdapter(adapter0);
        dropdown1.setAdapter(adapter1);

        noteTrack = new MidiTrack();


        play = (Button) findViewById(R.id.play);
        record0 = (ImageButton) findViewById(R.id.record_0);
        seek_bar0 = findViewById(R.id.seek_0);
        record1 = (ImageButton) findViewById(R.id.record_1);
        seek_bar1 = findViewById(R.id.seek_1);

        play.setEnabled(false);

        outputFile = getExternalCacheDir().getAbsolutePath();
        outputFile += "/audiorecordtest.3gp";
        System.out.println(outputFile);

        record0.setOnClickListener(new View.OnClickListener() {
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
                } catch (IOException ioe) {
                    Toast.makeText(getApplicationContext(), "io exception", Toast.LENGTH_LONG).show();
                }
                record0.setEnabled(false);
                record1.setEnabled(false);
                play.setEnabled(false);
                Toast.makeText(getApplicationContext(), "Recording started", Toast.LENGTH_LONG).show();
                ProgressThread p = new ProgressThread("recording", seek_bar0);
                p.start();
            }
        });

        record1.setOnClickListener(new View.OnClickListener() {
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
                } catch (IOException ioe) {
                    Toast.makeText(getApplicationContext(), "io exception", Toast.LENGTH_LONG).show();
                }
                record0.setEnabled(false);
                record1.setEnabled(false);
                play.setEnabled(false);
                Toast.makeText(getApplicationContext(), "Recording started", Toast.LENGTH_LONG).show();
                ProgressThread p = new ProgressThread("recording", seek_bar1);
                p.start();
            }
        });


        play.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mediaPlayer = new MediaPlayer();
                try {
                    record0.setEnabled(false);
                    record1.setEnabled(false);
                    play.setEnabled(false);

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


                    mediaPlayer.setDataSource(getExternalCacheDir().getAbsolutePath()+"/exported.mid");
                    mediaPlayer.prepare();
                    //mediaPlayer.start();
                    Toast.makeText(getApplicationContext(), "Playing Audio", Toast.LENGTH_LONG).show();
                    ProgressThread p = new ProgressThread("playing", seek_bar0, seek_bar1);
                    p.start();
                } catch (Exception e) {
                }
            }
        });
    }
}