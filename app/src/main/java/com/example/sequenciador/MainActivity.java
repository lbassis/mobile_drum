package com.example.sequenciador;

import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import androidx.core.app.ActivityCompat;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Handler;
import android.view.View;
import android.widget.AdapterView;
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
    private ImageButton play;
    private SeekBar seek_bar0, seek_bar1;
    private MediaRecorder myAudioRecorder;
    private String track1_file, track2_file;
    private Handler handler;
    private Boolean playing = false;
    private int pitch1 = 38;
    private int pitch2 = 113;

    volatile boolean running_thread = false;
    MidiTrack noteTrack0, noteTrack1;
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
            File file = new File(track1_file);
            if(file.exists()) {
                count = 0;
                amplituda.fromPath(track1_file)
                        .amplitudesAsList(list -> {
                            for (int tmp : list) {
                                count++;
                                if (tmp > 13) {
                                    System.out.println("som no momento" + count);
                                    noteTrack0.insertNote(9, pitch1, 100, (count - 1) * 128, 128);
                                }
//                            else {
//                                noteTrack0.insertNote(9, 0, 100, (count-1)*128, 128);
//                            }
                            }
                        });
            }
            file = new File(track2_file);
            if(file.exists()) {
                count = 0;
                amplituda.fromPath(track2_file)
                        .amplitudesAsList(list -> {
                            for (int tmp : list) {
                                count++;
                                if (tmp > 13) {

                                    System.out.println("som no momento" + count + " com pitch " + pitch2);
                                    noteTrack1.insertNote(9, pitch2, 100, (count - 1) * 128, 128);
                                }
                                //                            else {
                                //                                noteTrack1.insertNote(9, 0, 100, (count-1)*128, 128);
                                //                            }
                            }
                        });
            }
        }

        public void run() {

            if (mode == "playing") {
                while (running_thread) {
                    position = 0;
                    mediaPlayer.start();
                    while (position < 50 && running_thread) {
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
                try {
                    playing = false;
                    mediaPlayer.stop();
                    mediaPlayer.prepare();
                }
                catch (IOException e){}
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        record0.setEnabled(true);
                        record1.setEnabled(true);
                    }});
                        return;
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
                        play.setImageResource(R.drawable.play_button);
                        //Toast.makeText(getApplicationContext(), "Audio recorded successfully", Toast.LENGTH_LONG).show();
                    }
                    else if (mode == "playing") {
                        record0.setEnabled(true);
                        record1.setEnabled(true);
                        play.setEnabled(true);
                        play.setImageResource(R.drawable.play_button);
                    }
                }
            });
            }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        File fdelete = new File(getExternalCacheDir().getAbsolutePath()+"/track1.3gp");
        if (fdelete.exists()) {
            fdelete.delete();
        }
        fdelete = new File(getExternalCacheDir().getAbsolutePath()+"/track2.3gp");
        if (fdelete.exists()) {
            fdelete.delete();
        }

        //get the spinner from the xml.
        //38 -  42
        String[] spinner_items0 = new String[]{"Acoustic Snare", "Hand Clap", "Electric Snare", "Low Floor Tom", "Closed Hi-hat"};
        //String[] spinner_items1 = new String[]{"Tinkle Bell", "Agogo", "Steel Drums", "Woodblock", "Taiko Drum", "Melodic Tom", "Synth Drum"};

        Spinner dropdown0 = findViewById(R.id.spinner0);
        Spinner dropdown1 = findViewById(R.id.spinner1);

        ArrayAdapter<String> adapter0 = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, spinner_items0);
        ArrayAdapter<String> adapter1 = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, spinner_items0);

        dropdown0.setAdapter(adapter0);
        dropdown1.setAdapter(adapter1);

        dropdown0.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                pitch1 = position+38;// your code here
            }
            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
            }
        });
        dropdown1.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                pitch2 = position+38;
            }
            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
            }
        });

        noteTrack0 = new MidiTrack();
        noteTrack1 = new MidiTrack();

        play = (ImageButton) findViewById(R.id.play);
        record0 = (ImageButton) findViewById(R.id.record_0);
        seek_bar0 = findViewById(R.id.seek_0);
        record1 = (ImageButton) findViewById(R.id.record_1);
        seek_bar1 = findViewById(R.id.seek_1);

        play.setEnabled(false);

        track1_file = getExternalCacheDir().getAbsolutePath();
        track1_file += "/track1.3gp";
        track2_file = getExternalCacheDir().getAbsolutePath();
        track2_file += "/track2.3gp";

        record0.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                myAudioRecorder = new MediaRecorder();
                myAudioRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
                myAudioRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
                myAudioRecorder.setAudioEncoder(MediaRecorder.OutputFormat.AMR_NB);
                myAudioRecorder.setOutputFile(track1_file);

                try {
                    myAudioRecorder.prepare();
                    myAudioRecorder.start();

                } catch (IllegalStateException ise) {
                    //Toast.makeText(getApplicationContext(), "illegal state", Toast.LENGTH_LONG).show();
                } catch (IOException ioe) {
                    //Toast.makeText(getApplicationContext(), "io exception", Toast.LENGTH_LONG).show();
                }
                record0.setEnabled(false);
                record1.setEnabled(false);
                play.setEnabled(false);
                //Toast.makeText(getApplicationContext(), "Recording started", Toast.LENGTH_LONG).show();
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
                myAudioRecorder.setOutputFile(track2_file);

                try {
                    myAudioRecorder.prepare();
                    myAudioRecorder.start();

                } catch (IllegalStateException ise) {
                    //Toast.makeText(getApplicationContext(), "illegal state", Toast.LENGTH_LONG).show();
                } catch (IOException ioe) {
                    //Toast.makeText(getApplicationContext(), "io exception", Toast.LENGTH_LONG).show();
                }
                record0.setEnabled(false);
                record1.setEnabled(false);
                play.setEnabled(false);
                //Toast.makeText(getApplicationContext(), "Recording started", Toast.LENGTH_LONG).show();
                ProgressThread p = new ProgressThread("recording", seek_bar1);
                p.start();
            }
        });

        play.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!playing) {
                    playing = true;
                    mediaPlayer = new MediaPlayer();
                    try {
                        record0.setEnabled(false);
                        record1.setEnabled(false);
                        play.setImageResource(R.drawable.stop_button);
                        ArrayList<MidiTrack> tracks = new ArrayList<MidiTrack>();

                        tracks.add(noteTrack0);
                        tracks.add(noteTrack1);

                        MidiFile midi = new MidiFile(MidiFile.DEFAULT_RESOLUTION, tracks);

                        File output = new File(getExternalCacheDir().getAbsolutePath() + "/exported.mid");
                        try {
                            midi.writeToFile(output);
                        } catch (IOException e) {
                            System.err.println(e);
                        }


                        mediaPlayer.setDataSource(getExternalCacheDir().getAbsolutePath() + "/exported.mid");
                        mediaPlayer.prepare();
                        //mediaPlayer.start();
                        //Toast.makeText(getApplicationContext(), "Playing Audio", Toast.LENGTH_LONG).show();
                        ProgressThread p = new ProgressThread("playing", seek_bar0, seek_bar1);
                        running_thread = true;
                        p.start();
                    } catch (Exception e) {
                    }
                }
                else {
                    running_thread = false;
                    play.setImageResource(R.drawable.play_button);
                    seek_bar0.setProgress(0);
                    seek_bar1.setProgress(0);
                    playing = false;

                }
            }
        });
    }
}