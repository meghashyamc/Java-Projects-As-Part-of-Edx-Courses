package model;

import sound.MidiSynth;

import java.awt.*;

public class Oval extends Shape {


        public Oval(Point topLeft, MidiSynth midiSynth) {
            super(topLeft, midiSynth);
            PLAYING_COLOR = new Color(50, 161, 137);
            instrument = 100;
        }

    // EFFECTS: return true if this Oval contains the given point p, else return false

    @Override
    public boolean contains(Point p) {
        final double TOL = 1.0e-6;
        double halfWidth = width / 2.0;
        double halfHeight = height / 2.0;
        double diff = 0.0;

        if (halfWidth > 0.0) {
            diff = diff + sqrDiff(x + halfWidth, p.x) / (halfWidth * halfWidth);
        } else {
            diff = diff + sqrDiff(x + halfWidth, p.x);
        }
        if (halfHeight > 0.0) {
            diff = diff + sqrDiff(y + halfHeight, p.y) / (halfHeight * halfHeight);
        } else {
            diff = diff + sqrDiff(y + halfHeight, p.y);
        }
        return  diff <= 1.0 + TOL;
    }

    // Compute square of difference
// EFFECTS: returns the square of the difference of num1 and num2
    private double sqrDiff(double num1, double num2) {
        return (num1 - num2) * (num1 - num2);
    }




        //EFFECTS: draws the shape
        public void drawGraphics(Graphics g) {
            g.drawOval(x, y, width, height);
        }

        //EFFECTS: fills the shape
        public void fillGraphics(Graphics g) {
            g.fillOval(x, y, width, height);
        }

        // EFFECTS: starts playing this Shape, where sound is dependent on the area/coordinates of the Shape
        public void play(){
            int volume = areaToVelocity(width * height);
            midiSynth.play(instrument, coordToNote(y), volume);
        }

    }


