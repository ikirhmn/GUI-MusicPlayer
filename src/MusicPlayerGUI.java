import javax.sound.sampled.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
// Import library untuk UI dan audio
import java.awt.*;
import java.io.File;
import java.sql.*;
import java.util.ArrayList;
// Import library untuk manipulasi file, database, dan koleksi

public class MusicPlayerGUI extends JFrame {
    private JLabel songTitle, songArtist, thumbnailLabel, currentTimeLabel, totalTimeLabel;
    private JSlider progressBar;
    private JButton playButton;
    private ArrayList<Song> songdb; // Daftar lagu dari database
    private String currentSongPath; // Path file lagu yang sedang diputar
    private Clip audioClip; // Objek untuk memutar audio
    private boolean isPlaying = false; // Status pemutaran audio
    private boolean hasPlayedOnce = false; // Menyimpan status apakah musik pernah dimainkan
    private Thread progressThread; // Thread untuk progres bar

    public MusicPlayerGUI() {
        setTitle("Music Player"); // Set judul jendela
        setDefaultCloseOperation(EXIT_ON_CLOSE); // Atur aksi keluar aplikasi
        setSize(1000, 600);
        setResizable(false);
        setLayout(new BorderLayout()); // Gunakan layout BorderLayout

        // Panel Daftar Lagu
        JPanel songListPanel = new RoundedPanel(15); // Panel untuk daftar lagu
        songListPanel.setBounds(20, 20, 200, 500);
        songListPanel.setLayout(new BoxLayout(songListPanel, BoxLayout.Y_AXIS));
        songListPanel.setBackground(new Color(30, 30, 30)); // Warna latar belakang
        songListPanel.setBorder(new EmptyBorder(15, 15, 15, 15));

        // Memuat data lagu dari database
        loadSongsFromDatabase(songListPanel); // Muat daftar lagu dari database


        JScrollPane songScrollPane = new JScrollPane(songListPanel, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        songScrollPane.setBorder(BorderFactory.createEmptyBorder()); // Hilangkan border scroll pane
        songScrollPane.setOpaque(false);
        songScrollPane.getViewport().setOpaque(false);

        JScrollBar hBar = songScrollPane.getVerticalScrollBar();
        hBar.setPreferredSize(new Dimension(0, 0));// Hilangkan scrollbar horizontal
        hBar.setOpaque(false);

        // Panel Detail Lagu
        JPanel songDetailPanel = createSongDetailPanel();// Panel detail lagu

        add(songScrollPane, BorderLayout.WEST); // Tambahkan daftar lagu di kanan
        add(songDetailPanel, BorderLayout.CENTER);// Tambahkan detail lagu di tengah

        setLocationRelativeTo(null);  // Posisikan di tengah layar
        setVisible(true); // Tampilkan GUI
    }

    private void loadSongsFromDatabase(JPanel songListPanel) {
         // Muat lagu dari database dan tambahkan ke panel daftar lagu
        songdb = new ArrayList<>();
        try (Connection conn = DatabaseConnector.getConnection();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery("SELECT * FROM songs")) {

            while (rs.next()) {
                int id = rs.getInt("id");
                String title = rs.getString("title");
                String artist = rs.getString("artist");
                String songPath = rs.getString("song_path");
                String thumbnailPath = rs.getString("thumbnail_path");

                Song songs = new Song(id, title, artist, songPath, thumbnailPath);
                songdb.add(songs); // Tambahkan lagu ke daftar

                RoundedPanel songItemPanel = createSongItemPanel(songs); // Buat panel untuk lagu
                songListPanel.add(songItemPanel); // Tambahkan panel ke daftar lagu
                songListPanel.add(Box.createVerticalStrut(8)); // Tambahkan spasi antar lagu
            }
        } catch (SQLException e) {
            e.printStackTrace(); // Tampilkan error jika ada
        }
    }

    private RoundedPanel createSongItemPanel(Song song) {
         // Buat panel untuk setiap lagu
        RoundedPanel songItemPanel = new RoundedPanel(10);
        songItemPanel.setLayout(new BorderLayout());
        songItemPanel.setBackground(new Color(40, 40, 40)); // Warna latar belakang
        songItemPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60)); // Ukuran maksimum
        songItemPanel.setBorder(new EmptyBorder(8, 12, 8, 12));

        JPanel thumbnailPanel = new JPanel(new BorderLayout());
        thumbnailPanel.setBackground(new Color(0, 0, 0, 0));
        thumbnailPanel.setPreferredSize(new Dimension(44, 44));

        JLabel songThumbnail = new JLabel();
        File thumbnailFile = new File(song.getThumbnailPath());
        ImageIcon icon = thumbnailFile.exists()
                ? new ImageIcon(song.getThumbnailPath())
                : new ImageIcon("F:\\Kuliah\\Semester 5\\PBO\\PRAKTIKUM\\modul-4-jurnal-ikirhmn\\res\\images\\n" + //
                        "one.png");
        Image scaledImage = icon.getImage().getScaledInstance(44, 44, Image.SCALE_SMOOTH);
        songThumbnail.setIcon(new ImageIcon(scaledImage));
        thumbnailPanel.add(songThumbnail, BorderLayout.CENTER);

        JPanel textPanel = new JPanel();
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
        textPanel.setBackground(new Color(0, 0, 0, 0));
        textPanel.setBorder(new EmptyBorder(0, 12, 0, 0));

        JLabel titleLabel = new JLabel(song.getTitle());
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel artistLabel = new JLabel(song.getArtist());
        artistLabel.setForeground(new Color(160, 160, 160));
        artistLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        artistLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        textPanel.add(titleLabel);
        textPanel.add(Box.createVerticalStrut(2));
        textPanel.add(artistLabel);

        songItemPanel.add(thumbnailPanel, BorderLayout.WEST);
        songItemPanel.add(textPanel, BorderLayout.CENTER);

        songItemPanel.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                updateSongDetails(song);
            }
        });

        return songItemPanel;
    }

    private void startProgressThread() {
        if (progressThread != null && progressThread.isAlive()) {
            return; // Hindari memulai thread baru jika sudah berjalan
        }

        progressThread = new Thread(() -> {
            while (isPlaying && audioClip != null) {
                try {
                    // Mendapatkan posisi lagu saat ini (dalam detik)
                    long currentPosition = audioClip.getMicrosecondPosition() / 1_000_000;
                    long totalDuration = audioClip.getMicrosecondLength() / 1_000_000;

                    long currentMinutes = currentPosition / 60;
                    long currentSeconds = currentPosition % 60;
                    long totalMinutes = totalDuration / 60;
                    long totalSeconds = totalDuration % 60;

                    // Perbarui UI di thread Swing
                    progressBar.setValue((int) (currentPosition * 100 / totalDuration));

                    // Smooth transition for UI
                    SwingUtilities.invokeLater(() -> {
                        currentTimeLabel.setText(String.format("%02d:%02d", currentMinutes, currentSeconds));
                        totalTimeLabel.setText(String.format("%02d:%02d", totalMinutes, totalSeconds));
                        progressBar.repaint(); // Pastikan bar diperbarui di layar
                    });

                    // Jika lagu selesai, hentikan thread dan mainkan lagu berikutnya
                    if (currentPosition >= totalDuration) {
                        isPlaying = false;
                        SwingUtilities.invokeLater(() -> {
                            progressBar.setValue(0);
                            currentTimeLabel.setText("00:00");
                        });

                        playNextSong(); // Mainkan lagu berikutnya
                        break;
                    }

                    Thread.sleep(500); // Update setiap 500 ms
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        progressThread.start();
    }

    private void togglePlay() {
        try {
            if (audioClip == null || !audioClip.isOpen()) {
                File audioFile = new File(currentSongPath);
                if (!audioFile.exists()) {
                    JOptionPane.showMessageDialog(this, "Audio file not found!", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                AudioInputStream audioStream = AudioSystem.getAudioInputStream(audioFile);
                audioClip = AudioSystem.getClip();
                audioClip.open(audioStream);

                progressBar.setValue(0);
                long totalDuration = audioClip.getMicrosecondLength() / 1_000_000;
                long totalMinutes = totalDuration / 60;
                long totalSeconds = totalDuration % 60;
                totalTimeLabel.setText(String.format("%02d:%02d", totalMinutes, totalSeconds));
            }

            if (isPlaying) {
                audioClip.stop();
                playButton.setIcon(new ImageIcon(
                        "F:\\Kuliah\\Semester 5\\PBO\\PRAKTIKUM\\modul-4-jurnal-ikirhmn\\res\\images\\icon\\play.png"));
            } else {
                audioClip.start();
                playButton.setIcon(new ImageIcon(
                        "F:\\Kuliah\\Semester 5\\PBO\\PRAKTIKUM\\modul-4-jurnal-ikirhmn\\res\\images\\icon\\pause.png"));
                startProgressThread();

                // Tandai bahwa musik telah dimainkan setidaknya sekali
                hasPlayedOnce = true;
            }

            isPlaying = !isPlaying;

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void playNextSong() {
        if (songdb == null || songdb.isEmpty())
            return;

        // Cari lagu berikutnya
        for (int i = 0; i < songdb.size(); i++) {
            if (songdb.get(i).getSongPath().equals(currentSongPath)) {
                int nextIndex = (i + 1) % songdb.size(); // Jika terakhir, kembali ke awal
                updateSongDetails(songdb.get(nextIndex));
                break;
            }
        }
    }

    private void playPreviousSong() {
        if (songdb == null || songdb.isEmpty())
            return;

        // Cari lagu sebelumnya
        for (int i = 0; i < songdb.size(); i++) {
            if (songdb.get(i).getSongPath().equals(currentSongPath)) {
                int prevIndex = (i - 1 + songdb.size()) % songdb.size(); // Jika pertama, kembali ke akhir
                updateSongDetails(songdb.get(prevIndex));
                break;
            }
        }
    }

    private JPanel createSongDetailPanel() {
        JPanel songDetailPanel = new JPanel();
        songDetailPanel.setBackground(new Color(30, 30, 30));
        songDetailPanel.setLayout(new BorderLayout());
        songDetailPanel.setBorder(new EmptyBorder(20, 20, 20, 20));

        // Thumbnail
        thumbnailLabel = new JLabel();
        thumbnailLabel.setHorizontalAlignment(SwingConstants.CENTER);
        thumbnailLabel.setVerticalAlignment(SwingConstants.CENTER);
        thumbnailLabel.setPreferredSize(new Dimension(300, 300));
        thumbnailLabel.setOpaque(true);
        thumbnailLabel.setBackground(new Color(30, 30, 30)); // Warna latar belakang
        thumbnailLabel.setIcon(
                new ImageIcon("F:\\Kuliah\\Semester 5\\PBO\\PRAKTIKUM\\modul-4-jurnal-ikirhmn\\res\\images\\n" + //
                        "one.png"));
        songDetailPanel.add(thumbnailLabel, BorderLayout.NORTH);

        // Info Lagu
        songTitle = new JLabel("No Song Selected", SwingConstants.CENTER);
        songTitle.setForeground(Color.WHITE);
        songTitle.setFont(new Font("Segoe UI", Font.BOLD, 20));
        songTitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        songArtist = new JLabel("No Artist Selected", SwingConstants.CENTER);
        songArtist.setForeground(new Color(160, 160, 160));
        songArtist.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        songArtist.setAlignmentX(Component.CENTER_ALIGNMENT);

        JPanel songInfoPanel = new JPanel();
        songInfoPanel.setBackground(new Color(30, 30, 30));
        songInfoPanel.setLayout(new BoxLayout(songInfoPanel, BoxLayout.Y_AXIS));
        songInfoPanel.add(Box.createVerticalGlue());
        songInfoPanel.add(songTitle);
        songInfoPanel.add(Box.createVerticalStrut(8)); // Jarak antar elemen
        songInfoPanel.add(songArtist);
        songInfoPanel.add(Box.createVerticalGlue());

        songDetailPanel.add(songInfoPanel, BorderLayout.CENTER);

        // Panel Kontrol
        JPanel controlPanel = new JPanel(new BorderLayout());
        controlPanel.setBackground(new Color(40, 40, 40));
        controlPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        progressBar = new JSlider(0, 100, 0) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // Background bar
                g2.setColor(new Color(100, 100, 100));
                g2.fillRoundRect(0, getHeight() / 2 - 6, getWidth(), 12, 12, 12);

                // Filled progress
                int progressWidth = (int) (getWidth() * (getValue() / 100.0));
                g2.setColor(new Color(249, 172, 63));
                g2.fillRoundRect(0, getHeight() / 2 - 6, progressWidth, 12, 12, 12);
            }

            @Override
            public void updateUI() {
                // Disable UI updates to maintain custom appearance
            }
        };
        progressBar.setOpaque(false); // Transparan
        progressBar.setFocusable(false); // Nonaktifkan fokus keyboard
        progressBar.setBorder(null);
        progressBar.setBackground(new Color(40, 40, 40));
        progressBar.setEnabled(true); // Agar pengguna tidak bisa mengubah slider

        currentTimeLabel = new JLabel("00:00");
        currentTimeLabel.setForeground(Color.WHITE);
        currentTimeLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));

        totalTimeLabel = new JLabel("00:00");
        totalTimeLabel.setForeground(Color.WHITE);
        totalTimeLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));

        JPanel timePanel = new JPanel(new BorderLayout());
        timePanel.setOpaque(false);
        timePanel.add(currentTimeLabel, BorderLayout.WEST);
        timePanel.add(totalTimeLabel, BorderLayout.EAST);

        // Tombol Play
        playButton = new JButton();
        playButton.setPreferredSize(new Dimension(50, 50)); // Ukuran tombol
        playButton.setContentAreaFilled(false);
        playButton.setBorderPainted(false);
        playButton.setFocusPainted(false);

        // ActionListener untuk memanggil togglePlay()
        playButton.addActionListener(e -> togglePlay());

        // Panel untuk Tombol Kontrol
        JPanel buttonPanel = new JPanel();
        buttonPanel.setBackground(new Color(40, 40, 40));
        buttonPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 10, 15)); // Horizontal alignment dengan jarak 10px

        // Tombol Previous
        JButton previousButton = new JButton(new ImageIcon(
                "F:\\Kuliah\\Semester 5\\PBO\\PRAKTIKUM\\modul-4-jurnal-ikirhmn\\res\\images\\icon\\previous.png"));
        previousButton.setBorderPainted(false); // Menghilangkan border default
        previousButton.setContentAreaFilled(false); // Menghilangkan background
        previousButton.addActionListener(e -> playPreviousSong());

        // Tombol Play
        playButton = new JButton(new ImageIcon(
                "F:\\Kuliah\\Semester 5\\PBO\\PRAKTIKUM\\modul-4-jurnal-ikirhmn\\res\\images\\icon\\play.png"));
        playButton.setBorderPainted(false);
        playButton.setContentAreaFilled(false);
        playButton.addActionListener(e -> togglePlay());

        // Tombol Next
        JButton nextButton = new JButton(new ImageIcon(
                "F:\\Kuliah\\Semester 5\\PBO\\PRAKTIKUM\\modul-4-jurnal-ikirhmn\\res\\images\\icon\\next.png"));
        nextButton.setBorderPainted(false);
        nextButton.setContentAreaFilled(false);
        nextButton.addActionListener(e -> playNextSong());

        // Tambahkan tombol ke panel
        buttonPanel.add(previousButton);
        buttonPanel.add(playButton);
        buttonPanel.add(nextButton);

        // Tambahkan elemen ke panel controlButt
        controlPanel.add(timePanel, BorderLayout.NORTH);
        controlPanel.add(progressBar, BorderLayout.CENTER);
        controlPanel.add(buttonPanel, BorderLayout.SOUTH);

        songDetailPanel.add(controlPanel, BorderLayout.SOUTH);

        return songDetailPanel;
    }

    private void updateSongDetails(Song song) {
        try {
            // Hentikan musik yang sedang diputar
            if (audioClip != null && audioClip.isOpen()) {
                audioClip.stop();
                audioClip.close(); // Lepaskan resource
            }

            // Set detail lagu baru
            songTitle.setText(song.getTitle());
            songArtist.setText(song.getArtist());
            currentSongPath = song.getSongPath();

            // Tampilkan thumbnail baru
            File thumbnailFile = new File(song.getThumbnailPath());
            ImageIcon icon = thumbnailFile.exists()
                    ? new ImageIcon(song.getThumbnailPath())
                    : new ImageIcon(
                            "F:\\Kuliah\\Semester 5\\PBO\\PRAKTIKUM\\modul-4-jurnal-ikirhmn\\res\\images\\none.png");
            Image scaledImage = icon.getImage().getScaledInstance(300, 300, Image.SCALE_SMOOTH);
            thumbnailLabel.setIcon(new ImageIcon(scaledImage));

            // Muat file audio, tetapi jangan auto-play jika belum pernah dimainkan
            File audioFile = new File(currentSongPath);
            if (audioFile.exists()) {
                AudioInputStream audioStream = AudioSystem.getAudioInputStream(audioFile);
                audioClip = AudioSystem.getClip();
                audioClip.open(audioStream);

                if (hasPlayedOnce) {
                    // Auto-play jika musik pernah dimainkan
                    isPlaying = true;
                    audioClip.start();
                    playButton.setIcon(new ImageIcon(
                            "F:\\Kuliah\\Semester 5\\PBO\\PRAKTIKUM\\modul-4-jurnal-ikirhmn\\res\\images\\icon\\pause.png"));
                    startProgressThread();
                } else {
                    // Reset tombol play dan progress bar untuk lagu pertama
                    isPlaying = false;
                    progressBar.setValue(0);
                    currentTimeLabel.setText("00:00");
                    totalTimeLabel.setText("00:00");
                    playButton.setIcon(new ImageIcon(
                            "F:\\Kuliah\\Semester 5\\PBO\\PRAKTIKUM\\modul-4-jurnal-ikirhmn\\res\\images\\icon\\play.png"));
                }
            } else {
                JOptionPane.showMessageDialog(this, "Audio file not found!", "Error", JOptionPane.ERROR_MESSAGE);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(MusicPlayerGUI::new);
    }

}

