package cg.rickmorty;


import android.graphics.Bitmap;

public class Animation {

    private Bitmap[] frame;

    private int currentFrame;
    private long startTime;
    private long delay;
    private boolean playedOnce;

    public void setFrames(Bitmap[] frame) {
        this.frame = frame;
        currentFrame = 0;
        startTime = System.nanoTime();
    }

    public void setDelay(long d) {
        delay = d;
    }

    public void update() {
        long elapsed = (System.nanoTime() - startTime) / 1000000;

        if (elapsed > delay) {
            currentFrame++;
            startTime = System.nanoTime();
        }
        if (currentFrame == 1) {
            currentFrame = 0;
            playedOnce = true;
        }
    }

    public Bitmap getImage() {
        return frame[currentFrame];
    }

    public int getFrame() {
        return currentFrame;
    }

    public void setFrame(int i) {
        currentFrame = i;
    }

    public boolean playedOnce() {
        return playedOnce;
    }
}
