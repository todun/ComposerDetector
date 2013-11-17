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

        int N = Integer.parseInt(args[0]);
        String rootDir = args[1];
        if (!rootDir.endsWith("/"))
            rootDir = rootDir + "/";

        for (int arg = 2; arg < args.length; arg++) {

            File[] composer = new File(rootDir + args[arg]).listFiles(new FileFilter() {
                @Override
                public boolean accept(File pathName) {
                    return pathName.toString().endsWith("mid");
                }
            });
            for (File f : composer) {
                double[] noteMatrix = noteMatrixFromMidi(f, N);

                for (int i = 0; i < noteMatrix.length; i++) {
                    System.out.print(noteMatrix[i] + ",");
                }
                System.out.println(args[arg]);
            }
        }
    }

    private static double[] noteMatrixFromMidi(File f, int N) throws Exception {

        long noteCount = 0;
        double[] noteFrequency = new double[(int) Math.pow(12, N)];

        Sequence sequence = MidiSystem.getSequence(f);

        track:
        for (Track track : sequence.getTracks()) {
            for (int i = 0; i < track.size() - (N - 1); ) {

                int[] notes = new int[N];

                int count = 0;
                int found = -1;
                for (int j = 0; count < notes.length && i + j < track.size(); j++) {
                    MidiEvent e = track.get(i + j);
                    MidiMessage m = e.getMessage();
                    if (m instanceof ShortMessage) {
                        ShortMessage sm = (ShortMessage) m;
                        //note is on and velocity is not 0 (some assholes use 0 velocity to indicate note-off)
                        if (sm.getCommand() == NOTE_ON && sm.getData2() != 0) {
                            if (found < 0)
                                found = j;
                            notes[count++] = sm.getData1() % 12;
                        }

                    }
                }

                if (found < 0)
                    continue track;
                i += found + 1;

                int vectorPosition = 0;
                for (int j = 0; j < notes.length; j++) {
                    vectorPosition += notes[j] * Math.pow(12, j);
                }
                noteFrequency[vectorPosition]++;
                noteCount += notes.length;
            }
        }

        for (int i = 0; i < noteFrequency.length; i++)
            noteFrequency[i] /= noteCount / 2;

        return noteFrequency;
    }
}
