package net.wurmunlimited.forge.launcherfx;

import com.wurmonline.client.launcherfx.WurmStage;
import com.wurmonline.client.startup.ServerBrowserFX;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.TreeItemPropertyValueFactory;
import javafx.scene.layout.*;
import net.wurmunlimited.forge.config.ForgeClientConfig;
import net.wurmunlimited.forge.mods.Mod;
import net.wurmunlimited.forge.mods.ReleaseVersion;
import net.wurmunlimited.forge.mods.Repository;
import net.wurmunlimited.forge.mods.VersionHandler;
import net.wurmunlimited.forge.util.FileUtil;

import java.nio.file.Path;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.logging.Logger;
import java.util.stream.Collectors;

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
        private String active;


        ModReleaseVersion(Mod mod,ReleaseVersion rv) {
            this.mod = mod;
            this.releaseVersion = rv;
            this.name = (mod!=null && mod.name!=null? mod.name : "")+(rv!=null? " "+rv.tag : "");
            this.tag = rv!=null && rv.tag!=null? rv.tag : "";
            this.date = rv!=null? DATE_FORMAT.format(rv.date) : "";
            this.author = mod!=null && mod.author!=null? mod.author : "";
            this.installed = rv!=null && rv.isInstalled()? "yes" : "";
            this.active = rv!=null && rv.isInstalled()? "yes" : "";
        }

        ModReleaseVersion(String name) {
            this.mod = null;
            this.releaseVersion = null;
            this.name = name;
            this.tag = "";
            this.date = "";
            this.author = "";
            this.installed = "";
            this.active = "";
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

        public String getActive() {
            return active;
        }
    }

    public static ModsSettingsFX getInstance(boolean launcher) {
        instance.checkSettingsVisibility(launcher);
        instance.inLauncher = launcher;
        return instance;
    }

    private boolean inLauncher;
    public boolean closeSettings;
    private final TreeTableView<ModReleaseVersion> modsTable;
    private final ComboBox<String> modsComboBox;
    private final Button newButton;
    private final Button removeButton;
    private final Button installButton;
    private final Button disableButton;
    private final Button preferencesButton;
    private ServerBrowserFX launcherWindow;

    private ModsSettingsFX() {
        ForgeClientConfig config = ForgeClientConfig.getInstance();

        this.inLauncher = true;
        this.closeSettings = false;
        this.launcherWindow = null;
        this.setResizable(false);

        modsTable = new TreeTableView<>();
        TreeTableColumn<ModReleaseVersion,String> column1 = new TreeTableColumn<>("Mod Version");
        TreeTableColumn<ModReleaseVersion,String> column2 = new TreeTableColumn<>("Author");
        TreeTableColumn<ModReleaseVersion,String> column3 = new TreeTableColumn<>("Release Date");
        TreeTableColumn<ModReleaseVersion,String> column4 = new TreeTableColumn<>("Installed");
        TreeTableColumn<ModReleaseVersion,String> column5 = new TreeTableColumn<>("Active");
        column1.setCellValueFactory(new TreeItemPropertyValueFactory<>("name"));
        column1.prefWidthProperty().bind(modsTable.widthProperty().multiply(0.3));
        column2.setCellValueFactory(new TreeItemPropertyValueFactory<>("author"));
        column2.prefWidthProperty().bind(modsTable.widthProperty().multiply(0.15));
        column3.setCellValueFactory(new TreeItemPropertyValueFactory<>("date"));
        column3.prefWidthProperty().bind(modsTable.widthProperty().multiply(0.15));
        column3.setCellFactory(ModsSettingsFX::columnAlignRight);
        column4.setCellValueFactory(new TreeItemPropertyValueFactory<>("installed"));
        column4.prefWidthProperty().bind(modsTable.widthProperty().multiply(0.1));
        column4.setCellFactory(ModsSettingsFX::columnAlignCenter);
        column5.setCellValueFactory(new TreeItemPropertyValueFactory<>("active"));
        column5.prefWidthProperty().bind(modsTable.widthProperty().multiply(0.1));
        column5.setCellFactory(ModsSettingsFX::columnAlignCenter);
        modsTable.getColumns().addAll(column1,column2,column3,column4,column5);
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
        modsTable.setRoot(rootItem);
        modsTable.setShowRoot(false);

        List<Path> profileDirs = FileUtil.findDirectories(config.getModsProfilesDir(),false,false);
        List<String> profiles = profileDirs.stream().map(p -> p.getFileName().toString()).collect(Collectors.toList());
        String profile = config.getModsProfile();
        if(!profiles.contains(profile)) {
            profile = profiles.get(0);
            config.setModsProfile(profile);
        }
        Label profilesLabel = new Label("Profiles:");
        modsComboBox = new ComboBox<>();
        modsComboBox.setItems(FXCollections.observableArrayList(profiles));
        modsComboBox.getSelectionModel().select(profile);
        modsComboBox.setPrefWidth(160.0);
        newButton = new Button("New");
        newButton.setOnAction(this::onClickNewProfile);
        removeButton = new Button("Remove");
        removeButton.setOnAction(this::onClickRemoveProfile);
        removeButton.setDisable(profile.equals(config.getModsProfile()));
        modsComboBox.getSelectionModel().selectedItemProperty().addListener((options,previous,selected) -> {
            removeButton.setDisable(selected.equals(config.getModsProfile()));
            System.out.println(selected);
            modsTable.refresh();
        });

        Label modsLabel = new Label("Mods:");
        installButton = new Button("Install");
        installButton.setDisable(true);
        installButton.setOnAction(this::onClickInstallMod);
        disableButton = new Button("Disable");
        disableButton.setDisable(true);
        disableButton.setOnAction(this::onclickDisableMod);
        preferencesButton = new Button("Preferences");
        preferencesButton.setDisable(true);
        preferencesButton.setOnAction(this::onClickModPreferences);
        /*Button cancelButton = new Button("Cancel");
        cancelButton.setOnAction(this::onClickCancel);*/

        Region expander = new Region();
        HBox.setHgrow(expander,Priority.ALWAYS);
        HBox controlPanel = new HBox(NULL_SPACE_PERCENT);
        controlPanel.setAlignment(Pos.CENTER);
        controlPanel.getChildren().addAll(
            new Region(),
            profilesLabel,
            modsComboBox,
            newButton,
            removeButton,
            expander,
            modsLabel,
            installButton,
            disableButton,
            preferencesButton,
            /*cancelButton,*/
            new Region());
        VBox controlOuterPanel = new VBox(NULL_SPACE_PERCENT);
        controlOuterPanel.getChildren().addAll(controlPanel,new Region());
        BorderPane mainPanel = new BorderPane();
        mainPanel.setCenter(modsTable);
        BorderPane.setMargin(modsTable,new Insets(NULL_SPACE_PERCENT));
        mainPanel.setBottom(controlOuterPanel);
        this.setOnCloseRequest(event -> {
            event.consume();
            ModsSettingsFX.this.close();
        });
        modsTable.setRowFactory(this::onClickTableRow);
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
                        boolean isInstalled = rowData.releaseVersion.isInstalled();
                        boolean isActive = true;
                        installButton.setDisable(false);
                        installButton.setText(isInstalled? "Uninstall" : "Install");
                        preferencesButton.setDisable(!isInstalled);
                        disableButton.setDisable(!isInstalled);
                        disableButton.setText(!isInstalled || isActive? "Disable" : "Activate");
                    }
                } else if(event.getClickCount()==2) {
                    System.out.println("Double click on: "+rowData.getName());
                }
                return;
            }
            installButton.setDisable(true);
            preferencesButton.setDisable(true);
        });
        return row;
    }

    private void onClickNewProfile(ActionEvent evt) {
        TextInputDialog dialog = new TextInputDialog("");
        dialog.setTitle("New Mods Profile");
        dialog.setHeaderText("Create a new mods profile");
        dialog.setContentText("Enter the name of the profile:");
        String result = dialog.showAndWait().orElse(null);
        if(result==null) return;
        else if(result.isEmpty()) {
            System.out.println("Profile name can't be empty!");
        } else {
            System.out.println("Profile name: "+result);
        }
    }

    private void onClickRemoveProfile(ActionEvent evt) {
        String profile = modsComboBox.getValue();
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Remove Mods Profile");
        alert.setHeaderText("Remove the \""+profile+"\" mods profile?");
        alert.setContentText("Note, the configurations associated with the profile will be removed.");
        ButtonType result = alert.showAndWait().orElse(ButtonType.CANCEL);
        if(result==ButtonType.OK) {
            System.out.println("Profile "+profile+" removed.");
        }
    }

    private void onClickInstallMod(ActionEvent evt) {
    }

    private void onclickDisableMod(ActionEvent evt) {
    }

    private void onClickModPreferences(ActionEvent evt) {
    }

    /*public void onClickCancel(ActionEvent evt) {
        this.restart(evt);
        this.close();
    }*/

    @Override
    public void close() {
        super.close();
        if(!this.inLauncher) this.closeSettings = true;
    }
}

