import javax.sound.midi.*;
import java.io.File;
import java.io.FileFilter;

/**
 * Author: John Mooring (jmooring)
 * Date: 11/16/13
 * Time: 6:26 PM
 */
public class Main {
    private static final int NOTE_ON = 0x90;


    public static void main(String[] args) throws Exception {

        File[] beethoven = new File("").listFiles(new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                return pathname.toString().endsWith("mid");
            }
        });
        for (File f : beethoven) {
            double[] noteMatrix = noteMatrixFromMidi(f, 2);

            for (int i = 0; i < noteMatrix.length; i++)
                System.out.print(noteMatrix[i] + ",");
            System.out.println("beethoven");
        }
    }

    private static double[] noteMatrixFromMidi(File f, int N) throws Exception {

        long noteCount = 0;
        double[] noteFrequency = new double[(int) Math.pow(12, N)];

        Sequence sequence = MidiSystem.getSequence(f);

        for (Track track : sequence.getTracks()) {
            for (int i = 0; i < track.size() - (N - 1); i++) {

                //Is current note an on-note? If so increment note count
                {
                    MidiEvent e1 = track.get(i);
                    MidiMessage message = e1.getMessage();
                    if (message instanceof ShortMessage) {
                        ShortMessage sm = (ShortMessage) message;
                        //note is on and velocity is not 0 (some assholes use 0 velocity to indicate note-off)
                        if (sm.getCommand() == NOTE_ON && sm.getData2() != 0) {
                            noteCount++;
                        }
                    }
                }
                int vectorPosition = 1;
                for (int j = 0; j < N; j++) {
                    MidiEvent e1 = track.get(i + j);
                    MidiMessage message = e1.getMessage();
                    if (message instanceof ShortMessage) {
                        ShortMessage sm = (ShortMessage) message;
                        if (sm.getCommand() == NOTE_ON && sm.getData2() != 0) {
                            int key = sm.getData1();
                            int note = key % 12;
                            vectorPosition *= note;
                        }
                    }
                }
                noteFrequency[vectorPosition]++;
            }
        }

        for (int i = 0; i < noteFrequency.length; i++)
            noteFrequency[i] /= noteCount;

        return noteFrequency;
    }
}
