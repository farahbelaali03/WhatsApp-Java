package media;

public class MediaCapture {

    // ─── Attributes ───────────────────────────────────────────
    private AudioCapture audioCapture;
    private VideoCapture videoCapture;
    private UDPSender    udpSender;
    private UDPReceiver  udpReceiver;

    // ─── Constructor ──────────────────────────────────────────
    public MediaCapture() {
    }
    // ─── Getters ──────────────────────────────────────────────
    public AudioCapture getAudioCapture() { return audioCapture; }
    public VideoCapture getVideoCapture() { return videoCapture; }
    public UDPSender    getUdpSender()    { return udpSender; }
    public UDPReceiver  getUdpReceiver()  { return udpReceiver; }

    // ─── Methods ──────────────────────────────────────────────
    /public void start(String serverIP, String senderId, String recipientId) {
        this.udpSender    = new UDPSender(serverIP, 5001, 5002, senderId, recipientId);
        this.audioCapture = new AudioCapture(udpSender);
        this.videoCapture = new VideoCapture(udpSender);
        this.udpReceiver  = new UDPReceiver(); // ← no arguments anymore

        audioCapture.startCapture();
        videoCapture.startCapture();
        udpReceiver.startReceiving();

        System.out.println("MediaCapture: all streams started");
    }

    public void stop() {
        if (audioCapture != null) audioCapture.stopCapture();
        if (videoCapture != null) videoCapture.stopCapture();
        if (udpReceiver  != null) udpReceiver.stopReceiving();
        if (udpSender    != null) udpSender.close();

        System.out.println("MediaCapture: all streams stopped");
    }
}