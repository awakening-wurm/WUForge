package net.wurmunlimited.forge.launcherfx;

import com.wurmonline.client.launcherfx.WurmStage;
import com.wurmonline.client.startup.ServerBrowserFX;
import javafx.beans.value.ChangeListener;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.TreeItemPropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import net.wurmunlimited.forge.VersionHandler;
import net.wurmunlimited.forge.VersionHandler.Mod;
import net.wurmunlimited.forge.VersionHandler.ReleaseVersion;
import net.wurmunlimited.forge.VersionHandler.Repository;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.Locale;
import java.util.logging.Logger;

public class ModsSettingsFX extends WurmStage {

    private static final Logger logger = Logger.getLogger(ModsSettingsFX.class.getName());

    private static final int WIDTH = 800;
    private static final int HEIGHT = 600;
    private static final int LABEL_SPACE_PERCENT = 35;
    private static final int HELP_BUTTON_SPACE_PERCENT = 7;
    private static final int CONTROL_SPACE_PERCENT = 53;
    private static final int NULL_SPACE_PERCENT = 5;
    private static final double DEFAULT_SPACER_HEIGHT = 15.0;
    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd",Locale.ENGLISH);

    private static ChangeListener<? super Tab> lazyTabLoadingListener;

    private static final ModsSettingsFX instance = new ModsSettingsFX();

    static {
        instance.setTitle("Wurm Unlimited Forge - Mods Settings");
    }

    private static TreeTableCell<ModReleaseVersion,String> columnAlignCenter(TreeTableColumn<ModReleaseVersion,String> col) {
        TreeTableCell<ModReleaseVersion,String> cell = new TreeTableCell<ModReleaseVersion,String>() {
            @Override
            public void updateItem(String item,boolean empty) {
                super.updateItem(item,empty);
                if(empty) setText(null);
                else setText(item);
            }
        };
        cell.setAlignment(Pos.BASELINE_CENTER);
        return cell;
    }

    private static TreeTableCell<ModReleaseVersion,String> columnAlignRight(TreeTableColumn<ModReleaseVersion,String> col) {
        TreeTableCell<ModReleaseVersion,String> cell = new TreeTableCell<ModReleaseVersion,String>() {
            @Override
            public void updateItem(String item,boolean empty) {
                super.updateItem(item,empty);
                if(empty) setText(null);
                else setText(item);
            }
        };
        cell.setAlignment(Pos.BASELINE_RIGHT);
        return cell;
    }

    public static class ModReleaseVersion {
        public final Mod mod;
        public final ReleaseVersion releaseVersion;
        private String name;
        private String tag;
        private String date;
        private String author;
        private String installed;


        ModReleaseVersion(Mod mod,ReleaseVersion rv) {
            this.mod = mod;
            this.releaseVersion = rv;
            this.name = (mod!=null && mod.name!=null? mod.name : "")+(rv!=null? " "+rv.tag : "");
            this.tag = rv!=null && rv.tag!=null? rv.tag : "";
            this.date = rv!=null? DATE_FORMAT.format(rv.date) : "";
            this.author = mod!=null && mod.author!=null? mod.author : "";
            this.installed = rv!=null && rv.isInstalled()? "yes" : "";
        }

        ModReleaseVersion(String name) {
            this.mod = null;
            this.releaseVersion = null;
            this.name = name;
            this.tag = "";
            this.date = "";
            this.author = "";
            this.installed = "";
        }

        public String getName() {
            return name;
        }

        public String getDate() {
            return date;
        }

        public String getAuthor() {
            return author;
        }

        public String getInstalled() {
            return installed;
        }
    }

    public static ModsSettingsFX getInstance(boolean launcher) {
        instance.checkSettingsVisibility(launcher);
        instance.inLauncher = launcher;
        return instance;
    }

    private boolean inLauncher;
    public boolean closeSettings;
    private final Button installButton;
    private final Button preferencesButton;
    private ServerBrowserFX launcherWindow;

    private ModsSettingsFX() {
        this.inLauncher = true;
        this.closeSettings = false;
        this.launcherWindow = null;
        this.setResizable(false);

        /*String[] options = { "default","test","test2" };
        ComboBox<String> modsComboBox = new ComboBox<>();
        modsComboBox.setItems(FXCollections.observableArrayList(options));
        modsComboBox.getSelectionModel().select(options[0]);*/

        TreeTableView<ModReleaseVersion> table = new TreeTableView<>();
        TreeTableColumn<ModReleaseVersion,String> column1 = new TreeTableColumn<>("Mod Version");
        TreeTableColumn<ModReleaseVersion,String> column2 = new TreeTableColumn<>("Author");
        TreeTableColumn<ModReleaseVersion,String> column3 = new TreeTableColumn<>("Release Date");
        TreeTableColumn<ModReleaseVersion,String> column4 = new TreeTableColumn<>("Installed");
        column1.setCellValueFactory(new TreeItemPropertyValueFactory<>("name"));
        column1.prefWidthProperty().bind(table.widthProperty().multiply(0.3));
        column2.setCellValueFactory(new TreeItemPropertyValueFactory<>("author"));
        column2.prefWidthProperty().bind(table.widthProperty().multiply(0.15));
        column3.setCellValueFactory(new TreeItemPropertyValueFactory<>("date"));
        column3.prefWidthProperty().bind(table.widthProperty().multiply(0.15));
        column3.setCellFactory(ModsSettingsFX::columnAlignRight);
        column4.setCellValueFactory(new TreeItemPropertyValueFactory<>("installed"));
        column4.prefWidthProperty().bind(table.widthProperty().multiply(0.1));
        column4.setCellFactory(ModsSettingsFX::columnAlignCenter);
        table.getColumns().addAll(column1,column2,column3,column4);
        Repository repository = VersionHandler.getInstance().getRepository();
        final TreeItem<ModReleaseVersion> rootItem = new TreeItem<>(new ModReleaseVersion("Mods"));
        repository.mods.values().stream().sorted(Comparator.comparing(m -> m.name)).forEach(mod -> {
            final TreeItem<ModReleaseVersion> modItem = new TreeItem<>(new ModReleaseVersion(mod,mod.latest));
            logger.info("ModReleaseVersion: "+mod.name);
            mod.releases.stream()
                        .map(rv -> new ModReleaseVersion(mod,rv))
                        .filter(mrv -> mrv.releaseVersion!=mod.latest)
                        .sorted(Comparator.comparing(mrv -> mrv.date))
                        .forEach(mrv -> {
                            final TreeItem<ModReleaseVersion> rvItem = new TreeItem<>(mrv);
                            logger.info("ModReleaseVersion: +"+mrv.name+" ["+mrv.tag+","+mrv.author+"]");
                            modItem.getChildren().add(rvItem);
                        });
            rootItem.getChildren().add(modItem);
        });
        table.setRoot(rootItem);
        table.setShowRoot(false);

        installButton = new Button("Install");
        installButton.setDisable(true);
        installButton.setOnAction(this::installModReleaseVersion);
        preferencesButton = new Button("Preferences");
        preferencesButton.setDisable(true);
        preferencesButton.setOnAction(this::modPreferences);
        Button cancelButton = new Button("Cancel");
        cancelButton.setOnAction(this::restartAndClose);

        /*VBox modsPanel = new VBox(NULL_SPACE_PERCENT);
        modsPanel.getChildren().addAll(modsComboBox,treeTableView);*/
        HBox controlPanel = new HBox(NULL_SPACE_PERCENT);
        controlPanel.setAlignment(Pos.CENTER_RIGHT);
        controlPanel.getChildren().addAll(installButton,preferencesButton,cancelButton,new Region());
        VBox controlOuterPanel = new VBox(NULL_SPACE_PERCENT);
        controlOuterPanel.getChildren().addAll(controlPanel,new Region());
        BorderPane mainPanel = new BorderPane();
        mainPanel.setCenter(table);
        BorderPane.setMargin(table,new Insets(NULL_SPACE_PERCENT));
        mainPanel.setBottom(controlOuterPanel);
        this.setOnCloseRequest(event -> {
            event.consume();
            ModsSettingsFX.this.close();
        });
        table.setRowFactory(this::onClickTableRow);
        Scene scene = new Scene(mainPanel,WIDTH,HEIGHT);
        this.setScene(scene);
    }

    public void setLauncherWindow(ServerBrowserFX window) {
        this.launcherWindow = window;
    }

    private void checkSettingsVisibility(boolean launcher) {

    }

    public void restart() {
    }

    private void restart(ActionEvent evt) {
    }

    private TreeTableRow<ModReleaseVersion> onClickTableRow(TreeTableView<ModReleaseVersion> tv) {
        TreeTableRow<ModReleaseVersion> row = new TreeTableRow<>();
        row.setOnMouseClicked(event -> {
            if(!row.isEmpty()) {
                ModReleaseVersion rowData = row.getItem();
                if(event.getClickCount()==1) {
                    System.out.println("Single click on: "+rowData.getName());
                    if(rowData.releaseVersion!=null) {
                        installButton.setDisable(false);
                        installButton.setText(rowData.releaseVersion.isInstalled()? "Uninstall" : "Install");
                        preferencesButton.setDisable(!rowData.releaseVersion.isInstalled());
                        return;
                    }
                } else if(event.getClickCount()==2) {
                    System.out.println("Double click on: "+rowData.getName());
                    return;
                }
            }
            installButton.setDisable(true);
            preferencesButton.setDisable(true);
        });
        return row;
    }

    private void installModReleaseVersion(ActionEvent evt) {
    }

    private void modPreferences(ActionEvent evt) {
    }

    public void restartAndClose(ActionEvent evt) {
        this.restart(evt);
        this.close();
    }

    @Override
    public void close() {
        super.close();
        if(!this.inLauncher) this.closeSettings = true;
    }
}

