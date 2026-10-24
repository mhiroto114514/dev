package com.example.advisor.desktop;

import com.example.advisor.AdvisorApplication;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;

public final class DesktopLauncher {
    private final AtomicBoolean stopping = new AtomicBoolean();
    private volatile ConfigurableApplicationContext context;
    private JFrame window;
    private JLabel status;
    private JButton open;
    private URI address;
    private final Path dataDirectory;

    private DesktopLauncher(Path dataDirectory) {
        this.dataDirectory = dataDirectory;
    }

    public static void launch() {
        System.setProperty("java.awt.headless", "false");
        try {
            String local = System.getenv("LOCALAPPDATA");
            Path directory = local == null || local.isBlank()
                    ? Path.of(System.getProperty("user.home"), ".hanasaki-advisor")
                    : Path.of(local, "HanasakiAdvisor");
            Files.createDirectories(directory);
            InstanceLock lock = InstanceLock.acquire(directory.resolve("application.lock"));
            if (lock == null) {
                JOptionPane.showMessageDialog(null, "すでに起動しています。起動済みの「学力診断」を確認してください。",
                        "学力診断", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try { lock.close(); } catch (Exception ignored) { }
            }, "desktop-lock-release"));
            System.setProperty("logging.file.name", directory.resolve("application.log").toString());
            SwingUtilities.invokeLater(() -> new DesktopLauncher(directory).start());
        } catch (Exception error) {
            JOptionPane.showMessageDialog(null, "起動できませんでした。\n" + error.getMessage(),
                    "学力診断", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void start() {
        window = new JFrame("学力診断 / 花咲スクール");
        window.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        window.addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent event) { stop(); }
        });
        JPanel content = new JPanel(new BorderLayout(12, 16));
        content.setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));
        status = new JLabel("起動しています。しばらくお待ちください。");
        content.add(status, BorderLayout.NORTH);
        JTextArea help = new JTextArea("ブラウザを閉じてもアプリは動作しています。\n使い終わったら、このウィンドウの「終了」を押してください。\n成績は保存されません。元CSVは保管してください。");
        help.setEditable(false);
        help.setOpaque(false);
        help.setFont(UIManager.getFont("Label.font"));
        content.add(help, BorderLayout.CENTER);
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        open = new JButton("画面を開く");
        open.setEnabled(false);
        open.addActionListener(event -> browse());
        JButton exit = new JButton("終了");
        exit.addActionListener(event -> stop());
        buttons.add(open);
        buttons.add(exit);
        content.add(buttons, BorderLayout.SOUTH);
        window.setContentPane(content);
        window.pack();
        window.setMinimumSize(window.getSize());
        window.setLocationRelativeTo(null);
        window.setVisible(true);
        new Thread(this::startServer, "desktop-startup").start();
    }

    private void startServer() {
        try {
            SpringApplication application = new SpringApplication(AdvisorApplication.class);
            application.setHeadless(false);
            ConfigurableApplicationContext started = application.run(
                    "--spring.config.location=classpath:/application.properties",
                    "--spring.profiles.active=desktop", "--server.address=127.0.0.1", "--server.port=0",
                    "--advisor.school-master=classpath:school-master.csv");
            context = started;
            if (stopping.get()) {
                started.close();
                return;
            }
            int port = ((ServletWebServerApplicationContext) started).getWebServer().getPort();
            URI uri = URI.create("http://127.0.0.1:" + port + "/");
            {
                HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
                var response = client.send(HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(10)).GET().build(),
                        HttpResponse.BodyHandlers.discarding());
                if (response.statusCode() != 200) throw new IllegalStateException("画面を読み込めません: HTTP " + response.statusCode());
            }
            SwingUtilities.invokeLater(() -> {
                if (stopping.get()) return;
                address = uri;
                status.setText("起動しました。CSVを取り込んでご利用ください。");
                open.setEnabled(true);
                browse();
            });
        } catch (Exception error) {
            if (context != null) context.close();
            org.slf4j.LoggerFactory.getLogger(DesktopLauncher.class).error("Desktop startup failed", error);
            SwingUtilities.invokeLater(() -> {
                if (stopping.get()) return;
                status.setText("起動に失敗しました。「終了」で閉じてください。");
                JOptionPane.showMessageDialog(window, "起動に失敗しました。\n" + error.getMessage()
                        + "\nログ: " + dataDirectory.resolve("application.log"), "学力診断", JOptionPane.ERROR_MESSAGE);
            });
        }
    }

    private void browse() {
        URI uri = address;
        new Thread(() -> {
            try {
                Desktop.getDesktop().browse(uri);
            } catch (Exception error) {
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(window,
                        "ブラウザを開けませんでした。次のURLをブラウザで開いてください。\n" + uri,
                        "学力診断", JOptionPane.INFORMATION_MESSAGE));
            }
        }, "desktop-browser").start();
    }

    private void stop() {
        if (!stopping.compareAndSet(false, true)) return;
        open.setEnabled(false);
        status.setText("終了しています…");
        new Thread(() -> {
            // System.exit also runs Spring's shutdown hook if startup is still in progress.
            try {
                if (context != null) context.close();
            } finally {
                System.exit(0);
            }
        }, "desktop-shutdown").start();
    }
}
