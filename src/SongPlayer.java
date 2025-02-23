// import javax.sound.sampled.*;
// import java.io.File;

// public class SongPlayer {
//     private Clip audioClip;

//     public void play(String filePath) throws Exception {
//         if (audioClip != null && audioClip.isOpen()) {
//             audioClip.stop();
//             audioClip.close();
//         }
//         File audioFile = new File(filePath);
//         AudioInputStream audioStream = AudioSystem.getAudioInputStream(audioFile);
//         audioClip = AudioSystem.getClip();
//         audioClip.open(audioStream);
//         audioClip.start();
//     }

//     public void stop() {
//         if (audioClip != null && audioClip.isOpen()) {
//             audioClip.stop();
//         }
//     }

//     public boolean isPlaying() {
//         return audioClip != null && audioClip.isRunning();
//     }
// }
