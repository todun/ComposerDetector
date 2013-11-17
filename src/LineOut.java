
import javax.sound.sampled.*;

public class LineOut {
    private SourceDataLine soundLine;
    private byte[] buffer;
    private int bytes = 0;
    private static LineOut instance;
    private static TargetDataLine mic;

    public static long SAMPLING_RATE = 44100;




    // singleton constructor, only one lineout at a time.
    private LineOut() {
        try {
            try {
                Line.Info targetDLInfo = new Line.Info(TargetDataLine.class);
                Mixer.Info[] mixerInfo = AudioSystem.getMixerInfo();

                for (Mixer.Info info : mixerInfo) {
                    Mixer currentMixer = AudioSystem.getMixer(info);

                    if (currentMixer.isLineSupported(targetDLInfo)) {
                        mic = (TargetDataLine) currentMixer.getLine(targetDLInfo);
                    }
                }
                mic.open();
                mic.start();
            } catch (Exception e) {
                e.printStackTrace();
            }
            int channels = 2;
            AudioFormat audioFormat = new AudioFormat(SAMPLING_RATE, 16, channels,
                    true, false);
            buffer = new byte[2204 * channels];
            DataLine.Info info = new DataLine.Info(SourceDataLine.class,
                    audioFormat);
            soundLine = (SourceDataLine) AudioSystem.getLine(info);
            soundLine.open(audioFormat);
            soundLine.start();
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    public static LineOut getInstance() {
        if (instance == null)
            instance = new LineOut();
        return instance;
    }

    private void write(double d) {
        short s = (short) (d * Short.MAX_VALUE);
        buffer[bytes++] = (byte) (s & 0xff);
        buffer[bytes++] = (byte) ((s >> 8) & 0xff);
        if (bytes == buffer.length) {
            soundLine.write(buffer, 0, buffer.length);
            bytes = 0;
        }
    }

    public void writeM(double d) {
        write(d);
        write(d);
    }

    public void writeS(double d1, double d2) {
        write(d1);
        write(d2);
    }
}