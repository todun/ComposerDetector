import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;


public class Clip{
    private double[] audio;
    public boolean playing = false;
    private int loaded = 0;
    private int location = 0;
    private int max = 0;



    public Clip(final String f) {
        new Thread() {
            public void run() {
                AudioInputStream din = null;
                try {
                    File file = new File(f);
                    AudioInputStream in = AudioSystem.getAudioInputStream(file);
                    AudioFormat baseFormat = in.getFormat();
                    AudioFormat decodedFormat = new AudioFormat(
                            AudioFormat.Encoding.PCM_SIGNED,
                            baseFormat.getSampleRate(), 16, baseFormat.getChannels(),
                            baseFormat.getChannels() * 2, baseFormat.getSampleRate(),
                            false);
                    audio = new double[AudioSystem.getAudioFileFormat(file)
                            .getByteLength() * 8];
                    din = AudioSystem.getAudioInputStream(decodedFormat, in);

                    byte[] data = new byte[4096];

                    while (din.read(data, 0, data.length) != -1) {
                        ByteBuffer bf = ByteBuffer.wrap(data);
                        bf.order(ByteOrder.LITTLE_ENDIAN);

                        while (bf.hasRemaining()){
                            max++;
                            audio[loaded++] = (bf.getShort() / ((double) Short.MAX_VALUE));
                        }
                    }
                    loaded = Integer.MAX_VALUE;
                    din.close();


                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    if (din != null) {
                        try {
                            din.close();
                        } catch (IOException ignored) {
                        }
                    }
                }
            }
        }.start();
    }

    public void start() {
        location = 0;
        playing = true;
    }

    public void stop() {
        playing = false;
        location = 0;
    }

    public double read() {
        if (playing) {
            try {
                while(location >= loaded){
                    Thread.sleep(10);
                }
                if (location++ >= max) {
                    stop();
                    location = 0;
                }
                return audio[location];
            } catch (Exception e) {
                return 0;
            }
        } else
            return 0;
    }

}