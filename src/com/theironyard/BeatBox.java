package com.theironyard;

import java.awt.*;
import javax.swing.*;
import javax.sound.midi.*;
import java.awt.event.*;
import java.util.*;

public class BeatBox {

    JPanel mainPanel;
    ArrayList<JCheckBox> checkboxList;
    Sequencer sequencer;
    Sequence sequence;
    Track track;
    JFrame theFrame;

    String[] instrumentNames = {"Bass Drum", "Closed Hi-Hat", "Open Hi-Hat",
    "Acoustic Snare", "Crash Cymbal", "Hand Clap", "Cowbell", "Vibraslap",
    "Low-mid Tom", "High Agogo", "Open Hi-Conga"};
    int[] instruments = {35,42,46,38,49,39,50,60,70,72,64,56,58,47,67,63};
    //these represent the actual drum "keys". The drum channel is like a piano,
    //except each "key" on the piano is a different drum. So the number '35'
    //is the key for the Bass drum, 42 is Close Hi-Hat, etc.

    public static void main (String[] args) {
        new BeatBox().buildGUI();
    }


    public void buildGUI() {
        theFrame = new JFrame("Cyber BeatBox");
        theFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        BorderLayout layout = new BorderLayout();
        JPanel background = new JPanel(layout);
        background.setBorder(BorderFactory.createEmptyBorder(10,
                10,10,10));
        //an 'empty border' gives us a margin between the edges of the panel and
        //where the components are placed. Purely aesthetic.

        checkboxList = new ArrayList<>();
        Box buttonBox = new Box(BoxLayout.Y_AXIS);

        JButton start = new JButton("Start");
        start.addActionListener(new MyStartListener());
        buttonBox.add(start);

        JButton stop = new JButton("Stop");
        stop.addActionListener(new MyStopListener());
        buttonBox.add(stop);

        JButton upTempo = new JButton("Tempo up");
        upTempo.addActionListener(new MyUpTempoListener());
        buttonBox.add(upTempo);

        JButton downTempo = new JButton("Tempo Down");
        downTempo.addActionListener(new MyDownTempoListener());
        buttonBox.add(downTempo);

        Box nameBox = new Box(BoxLayout.Y_AXIS);
        for (int i = 0; i < 16; i++) {
            nameBox.add(new Label(instrumentNames[i]));
            //i < 16; threw ArrayIndexOutOfBoundsException.
            //changing to i < instrumentNames.length fixed it.
        }

        background.add(BorderLayout.EAST, buttonBox);
        background.add(BorderLayout.WEST, nameBox);

        theFrame.getContentPane().add(background);

        GridLayout grid = new GridLayout(16,16);
        grid.setVgap(1);
        grid.setHgap(2);
        mainPanel = new JPanel(grid);
        background.add(BorderLayout.CENTER, mainPanel);

        for (int i = 0; i < 256; i++); {
            JCheckBox c = new JCheckBox();
            c.setSelected(false);
            checkboxList.add(c);
            mainPanel.add(c);
            //Make the checkboxes, set them to 'false' (so they aren't checked)
            //and add them to the ArrayList AND to the GUI panel.
        }//end loop

        setUpMidi();

        theFrame.setBounds(50,50,300,300);
        theFrame.pack();
        theFrame.setVisible(true);
    }//close method

    public void setUpMidi() {
        try {
            sequencer = MidiSystem.getSequencer();
            sequencer.open();
            sequence = new Sequence(Sequence.PPQ, 4);
            track = sequence.createTrack();
            sequencer.setTempoInBPM(120);

        } catch (Exception e) {e.printStackTrace();}
    }//close MIDI method

    public void buildTrackAndStart() {
        int[] trackList = null;
        //make a 16-element array to hold the values for one instrument,
        //across all 16 beats. If the instrument is supposed to play on
        // that beat, the value at that element will be the key. If that
        // instrument is NOT supposed to play on that beat, put in a zero.

        sequence.deleteTrack(track);
        track = sequence.createTrack();
        //get rid of the old track, make a fresh one.

        for (int i = 0; i < 16; i++) {
            trackList = new int[16];
            //do this for each of the 16 rows(i.e. Bas, Conga, etc.)

            int key = instruments[i];
            //Set the 'key' that represents which instrument this is (Bass,
            //Hi-Hat, etc. The instruments array holds the actual MIDI numbers
            // for each instrument.)

            for (int j = 0; j < 16; j++) {

                JCheckBox jc = checkboxList.get(j + 16*i);
                if (jc.isSelected()) {
                    trackList[j] = key;
                } else {
                    trackList[j] = 0;
                } //if the checkbox at this beat is selected, put the key
                //value in this slot in the array. (the slot that represents
                //the beat.) Otherwise, the instrument is NOT supposed to play
                //at this beat, so set it to zero.
            }//close inner loop

            makeTracks(trackList);
            track.add(makeEvent(176, 1, 127, 0, 16));
            //For this instrument, and for all 16 beats,
            // make events and add them to the track.
        }//close outer

        track.add(makeEvent(192, 9, 1, 0, 15));
        //we always want to make sure that there IS an event at beat 16(it goes
        //0 to 15) Otherwise, the BeatBox might not go the full 16 beats before
        // it starts over.
        try {

            sequencer.setSequence(sequence);
            sequencer.setLoopCount(sequencer.LOOP_CONTINUOUSLY);
            //Lets you specify the number of loop iterations, or in this case
            //continuous looping
            sequencer.start();
            sequencer.setTempoInBPM(120);
        } catch (Exception e) { e.printStackTrace(); }
        //now play it!
    }//close buildTrackAndStart method

        public class MyStartListener implements ActionListener {
            public void actionPerformed(ActionEvent a) {
                buildTrackAndStart();
            }
        }//close inner class

        public class MyStopListener implements ActionListener {
            public void actionPerformed(ActionEvent a) {
             sequencer.stop();
            }
        }//close inner class

        public class MyUpTempoListener implements ActionListener {
            public void actionPerformed(ActionEvent a) {
                float tempoFactor = sequencer.getTempoFactor();
                sequencer.setTempoFactor((float)(tempoFactor * 1.03));
                //The Tempo Factor scales teh sequencer's tempo by the factor
                //provided. The default is I.O, so we're adjusting +/-3%
                //per click.
            }
        }//close inner class

        public class MyDownTempoListener implements ActionListener {
            public void actionPerformed(ActionEvent a) {
                float tempoFactor = sequencer.getTempoFactor();
                sequencer.setTempoFactor((float)(tempoFactor * .97));
            }
        }
        public void makeTracks(int[] list) {
            //Makes events for one instrument at a teime, for all 16 beats. So it
        //might get an int[] for the Bass drum, and each index in the array will
        //hold either the key of that instrument, or a zero. If it's a zero, the
        //instrument isn't supposed to play at that beat. Otherwise, make an event
        //and add it to the track.

            for (int i = 0; i < 16; i++) {
                int key = list[i];
                if (key !=0) {
                    track.add(makeEvent(144,9,key,100,i));
                    track.add(makeEvent(128,9,key,100,i+1));
                    //make the NOTE ON and NOTE OFF events, and add
                    // them to the track.
                }
            }
    }//close makeTracks method

            public MidiEvent makeEvent(int comd, int chan, int one,
                  int two, int tick) {
            MidiEvent event = null;
            try {
                ShortMessage a = new ShortMessage();
                a.setMessage(comd, chan, one, two);
                event = new MidiEvent(a, tick);

            }catch(Exception e) {e.printStackTrace();}
            return event;
            }
    }//close class



