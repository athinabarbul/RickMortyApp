package cg.rickmorty;


import android.content.Context;
import android.media.AudioManager;
import android.media.SoundPool;

public class SoundPlayer {

    private static SoundPool soundPool;
    private static int overSound;
    private static int hitSound;
    private static int bgSound;

    public SoundPlayer(Context context) {
        soundPool = new SoundPool(2, AudioManager.STREAM_MUSIC, 0);
        hitSound = soundPool.load(context, R.raw.rickhit, 1);
        overSound = soundPool.load(context, R.raw.ricksay, 1);
        bgSound = soundPool.load(context, R.raw.bg, 1);

    }

    public void playBackgroundSound() {
        soundPool.play(bgSound, 1.0f, 1.0f, 1, 1, 1.0f);
    }

    public void playHitSound() {
        soundPool.play(hitSound, 1.0f, 1.0f, 1, 0, 1.0f);
    }

    public void playOverSound() {
        soundPool.play(overSound, 1.0f, 1.0f, 1, 0, 1.0f);
    }
}
