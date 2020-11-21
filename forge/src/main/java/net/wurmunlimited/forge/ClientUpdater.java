package net.wurmunlimited.forge;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import net.wurmunlimited.forge.config.ForgeClientConfig;
import net.wurmunlimited.forge.config.ForgeConfig;
import net.wurmunlimited.forge.interfaces.ForgeUpdater;
import net.wurmunlimited.forge.mods.ReleaseVersion;
import net.wurmunlimited.forge.mods.VersionHandler;
import net.wurmunlimited.forge.util.FileUtil;
import net.wurmunlimited.forge.util.HttpClient;

import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;


public class ClientUpdater extends Application implements ForgeUpdater {

    private static final Logger logger = Logger.getLogger(ClientUpdater.class.getName());

    private static final double PROGRESS_INCREASE = 0.01d;
    private static final double PROGRESS_MAX = 1.0d;

    public static void main(final String[] args) {
        System.out.println("ClientUpdater: main()...");
        launch(args);
    }

    private Label statusLabel;
    private double progressFrom;
    private double progressTo;
    private String progressMessage;
    private String progressStatus;

    @Override
    public void start(Stage stage) {
        System.out.println("ClientUpdater: start...");
        updateProgress(0.0d,0.1d,"Running update...");
        statusLabel = new Label();

        Task<Void> installTask = new Task<Void>() {
            @Override
            public Void call() {
                double p;
                while(true) {
                    if(progressStatus==null || !progressStatus.equals(progressMessage)) {
                        progressStatus = progressMessage;
                        Platform.runLater(() -> statusLabel.setText(progressStatus));
                    }
                    p = progressFrom;
                    updateProgress(p,PROGRESS_MAX);
                    if(p>=PROGRESS_MAX) break;
                    if(progressFrom<progressTo) progressFrom += PROGRESS_INCREASE;
                    try {
                        Thread.sleep(200L);
                    } catch(InterruptedException e) {}
                }
                return null;
            }
        };

        ProgressBar progressBar = new ProgressBar();
        progressBar.progressProperty().bind(installTask.progressProperty());
        VBox root = new VBox(statusLabel,progressBar);
        root.setFillWidth(true);
        root.setAlignment(Pos.CENTER);

        Scene scene = new Scene(root,300,150);
        stage.setScene(scene);
        stage.setTitle("Wurm Unlimited Forge Intaller");
        stage.show();

        installTask.setOnSucceeded(event -> {
            System.out.println("Finish");
        });
        Thread installingThread = new Thread(installTask);
        installingThread.start();
        new Thread(() -> runInstaller()).start();
    }

    private void runInstaller() {
        System.out.println("ClientUpdater: runInstaller...");
        updateProgress(0.1d,0.15d,null);
        Path baseDir = Paths.get("").toAbsolutePath().getParent();
        Properties properties = FileUtil.loadProperties(baseDir.resolve("forge.properties"));
        ForgeClientConfig.init(baseDir,properties);
        if(VersionHandler.getInstance().installUpdates(this)) {
            /*JPanel panel = new JPanel();
            panel.setLayout(new GridLayout(2,1));
            panel.setBounds(0,0,468,120);
            JLabel l = new JLabel("Your client mods have been updated, please restart your Wurm Unlimited client.");
            l.setVerticalTextPosition(JButton.TOP);
            l.setHorizontalTextPosition(JButton.CENTER);
            panel.add(l);
            JOptionPane.showMessageDialog(null,panel,"Client Mod Update",JOptionPane.PLAIN_MESSAGE);*/
        }
        updateProgress(1.0d,1.0d,"Finished.");
        System.out.println("ClientUpdater: Done.");
    }

    @Override
    public void updateProgress(double from,double to,String status) {
        progressFrom = from;
        progressTo = to;
        if(status!=null) progressMessage = status;
    }

    @Override
    public boolean updateForge(ReleaseVersion releaseVersion) {
        updateProgress(0.15d,0.8d,"Downloading...");
        ForgeConfig config = ForgeConfig.getInstance();
        String zipUrl = releaseVersion.getZipUrl();
        Path zipFile = config.getCacheDir().resolve(releaseVersion.file);
        logger.info("Download forge: "+zipUrl+" => "+zipFile.toAbsolutePath().toString());
        if(Files.exists(zipFile) || HttpClient.download(zipUrl,zipFile)) {
            updateProgress(0.8d,1.0d,"Installing...");
            try {
                if(extractForge(zipFile)) {
                    VersionHandler.getInstance().updateForgeVersion(releaseVersion);
                    return true;
                }
            } catch(IOException e) {
                logger.log(Level.SEVERE,e.getMessage(),e);
            }
        }
        return false;
    }

    private boolean extractForge(Path zipFile) throws IOException {
        int num = 0;
        ForgeClientConfig config = ForgeClientConfig.getInstance();
        try(ZipInputStream zipIn = new ZipInputStream(new FileInputStream(zipFile.toFile()))) {
            ZipEntry entry;
            while((entry = zipIn.getNextEntry())!=null) {
                String fileName = entry.getName();
                int i1 = fileName.lastIndexOf('/');
                int i2 = fileName.lastIndexOf('\\');
                if(i1!=-1 || i2!=-1) fileName = fileName.substring((i1>i2? i1 : i2)+1);
                logger.info("extractForge: "+entry.getName()+" ["+fileName+"]");
                if(!entry.isDirectory()) {
                    Path filePath;
                    if(fileName.equals("client.jar")) filePath = config.getClientJar();
                    else if(fileName.equals("forge.jar")) filePath = config.getUpdateForgeJar();
                    else continue;
                    try(BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(filePath.toFile()))) {
                        byte[] bytesIn = new byte[2048];
                        int read = 0;
                        while((read = zipIn.read(bytesIn))!=-1) {
                            bos.write(bytesIn,0,read);
                        }
                    }
                    Files.setLastModifiedTime(filePath,FileTime.fromMillis(entry.getTime()));
                    ++num;
                }
                zipIn.closeEntry();
            }
        }
        return num>0;
    }
}
