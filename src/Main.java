import javax.sound.midi.*;
import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.FileFilter;
import java.io.PrintWriter;
import java.util.ArrayList;

import weka.classifiers.Classifier;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.converters.ArffLoader;

/**
 * Author: John Mooring (jmooring)
 * Date: 11/16/13
 * Time: 6:26 PM
 */
public class Main {

    private static final int CLOCK = 12;
    private static final int DURATION_BUCKETS = 8;
    private static final int SPLITS = 5;
    private static double[][] fd;
    private static boolean[] keys = new boolean[127];
    private static long time = 0;
    private static String[] composers = new String[]{"beethoven", "brahms", "chopin", "grieg", "mozart"};


    public static void main(String[] args) throws Exception {
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception e) {
            //suppress
        }
        JFileChooser fc = new JFileChooser("/home/john");
        fc.showOpenDialog(null);
        File f = fc.getSelectedFile();
        if (f == null)
            return;
        if (!f.toString().endsWith("mid")) {
            convertToMidi(f, new File("conv.mid"));
            f = new File("conv.mid");
        }
        generateSingleSet(2, f, new File("/home/john/Desktop/test.arff"),
                composers, "mozart");
        System.out.println(composers[classify(new File("/home/john/Desktop/test.arff"))]);
    }

    private static void convertToMidi(File in, File output) throws Exception {
        LineOut out = LineOut.getInstance();
        Clip clip = new Clip(in.getAbsolutePath());
        clip.start();


        Sequence sequence = new Sequence(Sequence.PPQ, 24);
        final Track t = sequence.createTrack();

        JFrame gui = new JFrame();
        gui.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        gui.add(new JPanel() {
            public void paint(Graphics g) {
                try {
                    g.setColor(Color.black);
                    g.fillRect(0, 0, getWidth(), getHeight());
                    if (fd == null)
                        return;
                    int[] xpoints = new int[fd[0].length + 2];
                    int[] ypoints = new int[fd[0].length + 2];
                    xpoints[0] = 0;
                    xpoints[xpoints.length - 1] = getWidth();
                    ypoints[0] = getHeight();
                    ypoints[ypoints.length - 1] = getHeight();

                    for (int i = 0; i < fd[0].length; i++) {
                        xpoints[i + 1] = (int) ((getWidth() * fd[0][i]) / fd[0][fd[0].length - 1]);
                        ypoints[i + 1] = (int) (getHeight() - Math.sqrt(fd[1][i]) * getHeight() * 100);
                    }

                    g.setColor(Color.green);
                    g.fillPolygon(xpoints, ypoints, xpoints.length);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        gui.setSize(500, 500);
        gui.setVisible(true);
        double[] buffer = new double[4096 * 8];
        int pos = 0;
        while (clip.playing) {
            double left = clip.read();
            double right = clip.read();
            double avg = (left + right) / 2;
            buffer[pos++] = avg;
            if (pos >= buffer.length - 1) {
                pos = (3 * buffer.length) / 4;
                fd = FFT.FrequencyDomain(buffer);
                time += 10;
                for (int i = 0; i < fd[0].length; ) {
                    double power = fd[1][i];
                    int midi = freqToMidi(fd[0][i]);
                    if (midi > 126 || midi < 59) {
                        i++;
                        continue;
                    }
                    while (freqToMidi(fd[0][++i]) == midi)
                        power += fd[1][i];
                    if (Math.sqrt(power) * 100 > 0.25) {
                        if (!keys[midi]) {
                            keys[midi] = true;
                            ShortMessage sm = new ShortMessage();
                            sm.setMessage(ShortMessage.NOTE_ON, midi, (int) (120 * (power + 0.6)));
                            MidiEvent e = new MidiEvent(sm, time);
                            t.add(e);
                        }
                    }
                    if (Math.sqrt(power) * 100 < 0.15) {
                        if (keys[midi]) {
                            keys[midi] = false;
                            ShortMessage sm = new ShortMessage();
                            sm.setMessage(ShortMessage.NOTE_ON, midi, 0);
                            MidiEvent e = new MidiEvent(sm, time);
                            t.add(e);
                        }
                    }
                }
                gui.repaint();
                for (int i = 0; i < (3 * buffer.length) / 4; i++)
                    buffer[i] = buffer[i + buffer.length / 4];
            }
            //out.writeS(left, right);
        }
        MidiSystem.write(sequence, MidiSystem.getMidiFileTypes(sequence)[0], output);
    }

    private static int classify(File item) throws Exception {
        int[] cts = new int[5];
        ArffLoader loader = new ArffLoader();
        loader.setFile(item);
        Instances ins = loader.getStructure();
        ins.setClassIndex(ins.numAttributes() - 1);
        Classifier cls_co = (Classifier) weka.core.SerializationHelper
                .read("/home/john/Desktop/weka-3-6-10/NB.model");
        for (; ; ) {
            Instance in = loader.getNextInstance(ins);
            if (in == null)
                break;
            cts[(int) cls_co.classifyInstance(in)]++;
        }
        int mi = 0;
        for (int i = 0; i < cts.length; i++)
            if (cts[mi] < cts[i])
                mi = i;
        return mi;
    }

    private static int freqToMidi(double d) {
        int mid = (int) (69 + 12 * (Math.log(d / 440) / Math.log(2)));
        return mid < 0 ? 0 : mid;
    }

    private static void generateSingleSet(int N, File in, File outFile, String[] composers, String composer) throws Exception {
        PrintWriter out = new PrintWriter(outFile);
        out.println("@RELATION composer");
        //features
        for (int i = 0; i < Math.pow(CLOCK, N) + DURATION_BUCKETS; i++)
            out.println("@ATTRIBUTE a" + i + " REAL");
        //class
        out.print("@ATTRIBUTE class {");
        for (int i = 0; i < composers.length - 1; i++)
            out.print(composers[i] + ",");
        out.println(composers[composers.length - 1] + "}");

        out.println("@DATA");


        for (int n = 0; n < SPLITS; n++) {
            double[] noteMatrix = noteMatrixFromMidi(in, N, SPLITS, n);

            for (int i = 0; i < noteMatrix.length; i++) {
                out.print(noteMatrix[i] + ",");
            }
            out.println(composer);

        }
        out.flush();
        out.close();
    }

    private static void generateFeatureSet(String[] args) throws Exception {
        int N = Integer.parseInt(args[0]);

        System.out.println("@RELATION composer");
        //features
        for (int i = 0; i < Math.pow(CLOCK, N) + DURATION_BUCKETS; i++)
            System.out.println("@ATTRIBUTE a" + i + " REAL");
        //class
        System.out.print("@ATTRIBUTE class {");
        for (int i = 2; i < args.length - 1; i++)
            System.out.print(args[i] + ",");
        System.out.println(args[args.length - 1] + "}");

        System.out.println("@DATA");

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
                for (int n = 0; n < SPLITS; n++) {
                    double[] noteMatrix = noteMatrixFromMidi(f, N, SPLITS, n);

                    for (int i = 0; i < noteMatrix.length; i++) {
                        System.out.print(noteMatrix[i] + ",");
                    }
                    System.out.println(args[arg]);
                }
            }
        }
    }

    private static double[] noteMatrixFromMidi(File f, int N, int split, int num) throws Exception {

        long noteCount = 0;
        double[] noteFrequency = new double[(int) Math.pow(CLOCK, N) + DURATION_BUCKETS];
        ArrayList<Double> durations = new ArrayList<Double>();

        Sequence sequence = MidiSystem.getSequence(f);

        track:
        for (Track track : sequence.getTracks()) {
            for (int i = num * track.size() / split;
                 i < (num + 1) * track.size() / split - (N - 1); ) {


                {
                    MidiEvent e = track.get(i);
                    MidiMessage m = e.getMessage();
                    if (m instanceof ShortMessage) {
                        ShortMessage sm = (ShortMessage) m;
                        //note is on and velocity is not 0 (some assholes use 0 velocity to indicate note-off)
                        if (sm.getCommand() == ShortMessage.NOTE_ON && sm.getData2() != 0) {
                            durations.add(Math.pow(2, duration(i, track)));
                        }
                    }
                }

                int[] notes = new int[N];

                int count = 0;
                int found = -1;
                for (int j = 0; count < notes.length && i + j < track.size(); j++) {
                    MidiEvent e = track.get(i + j);
                    MidiMessage m = e.getMessage();
                    if (m instanceof ShortMessage) {
                        ShortMessage sm = (ShortMessage) m;
                        //note is on and velocity is not 0 (some assholes use 0 velocity to indicate note-off)
                        if (sm.getCommand() == ShortMessage.NOTE_ON && sm.getData2() != 0) {
                            if (found < 0)
                                found = j;
                            notes[count++] = sm.getData1() % CLOCK;
                        }

                    }
                }

                if (found < 0)
                    continue track;
                i += found + 1;  //skip over empties so we don't duplicate tuples

                int vectorPosition = 0;
                for (int j = 0; j < notes.length; j++) {
                    vectorPosition += notes[j] * Math.pow(CLOCK, j);
                }

                noteFrequency[vectorPosition]++;
                noteCount += notes.length;
            }
        }

        for (int i = 0; i < noteFrequency.length; i++)
            noteFrequency[i] /= (noteCount / N);

        double max = 0;
        for (double d : durations)
            if (d > max)
                max = d;

        for (double d : durations) {
            int pos = (int) (DURATION_BUCKETS * d / max);
            noteFrequency[noteFrequency.length - DURATION_BUCKETS - 1 + pos]++;
        }

        for (int i = noteFrequency.length - DURATION_BUCKETS; i < noteFrequency.length; i++)
            noteFrequency[i] /= durations.size();

        return noteFrequency;
    }

    private static double duration(int pos, Track track) {
        MidiEvent startEvent = track.get(pos);
        MidiMessage startMM = startEvent.getMessage();
        int note;
        if (startMM instanceof ShortMessage) {
            ShortMessage sm = (ShortMessage) startMM;
            note = sm.getData1();
            if (sm.getCommand() == ShortMessage.NOTE_OFF) {
                System.exit(1);
            }
        } else {
            return 0;
        }

        long start = startEvent.getTick();


        for (int j = pos; j < track.size(); j++) {
            MidiEvent e = track.get(j);
            MidiMessage m = e.getMessage();
            if (m instanceof ShortMessage) {
                ShortMessage sm = (ShortMessage) m;
                if (note == sm.getData1() && (sm.getCommand() == ShortMessage.NOTE_OFF || sm.getData2() == 0)) {
                    return e.getTick() - start;
                }
            }
        }

        return 0;
    }
}
