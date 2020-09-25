package net.wurmunlimited.forge;

import javassist.*;
import javassist.bytecode.AnnotationsAttribute;
import javassist.bytecode.ClassFile;
import javassist.bytecode.ConstPool;
import javassist.bytecode.annotation.Annotation;
import javassist.expr.ExprEditor;
import javassist.expr.MethodCall;
import net.wurmunlimited.forge.util.Syringe;

import java.util.logging.Level;
import java.util.logging.Logger;


public class CodeInjections {

    private static final Logger logger = Logger.getLogger(CodeInjections.class.getName());

    public static void preInit() {
        Syringe sbfx = Syringe.getSyringe("com.wurmonline.client.startup.ServerBrowserFX");
        sbfx.addField("private javafx.scene.control.ComboBox modsConfigBox;","null");
        sbfx.addMethod("public void changeModsConfig() { WUForge.changeModsConfig(this.modsConfigBox); }");
        sbfx.addMethod("public void launchModsSettings() { WUForge.launchModsSettings(this.modsConfigBox); }");
        sbfx.insertAfter("initialize","WUForge.ServerBrowserFX_initialize(this.modsConfigBox);",null);
        sbfx.instrument(null,"(Ljavafx/application/Application;Ljava/lang/Runnable;)V",new ExprEditor() {
            int i = 0;

            @Override
            public void edit(MethodCall mc) throws CannotCompileException {
                if(mc.getMethodName().equals("getResource")) {
                    if(i==1 || i==2 || i==3) {
                        mc.replace("$_ = WUForge.getResource($$);");
                        logger.info("WUForge: Change to custom server browser.");
                    }
                    ++i;
                }
            }
        });
        sbfx.setBody("launchForums","this.application.getHostServices().showDocument(\"http://forum.wurmonline.com\");",null);
        sbfx.setBody("launchWurmpedia","this.application.getHostServices().showDocument(\"http://www.wurmpedia.com/index.php/Main_Page\");",null);
        sbfx.setBody("launchCredits","this.application.getHostServices().showDocument(WUForge.getCreditsURL());",null);
        try {
            CtClass ctClass = sbfx.getCtClass();
            ClassFile classFile = sbfx.getCtClass().getClassFile();
            ConstPool constpool = classFile.getConstPool();
            AnnotationsAttribute annotationsAttribute = new AnnotationsAttribute(constpool,AnnotationsAttribute.visibleTag);
            Annotation annotationFXML = new Annotation("javafx.fxml.FXML",constpool);
            annotationsAttribute.addAnnotation(annotationFXML);
            CtField modsConfigBox = ctClass.getField("modsConfigBox");
            modsConfigBox.getFieldInfo().addAttribute(annotationsAttribute);
            CtMethod changeModsConfig = ctClass.getMethod("changeModsConfig","()V");
            changeModsConfig.getMethodInfo().addAttribute(annotationsAttribute);
            CtMethod launchModsSettings = ctClass.getMethod("launchModsSettings","()V");
            launchModsSettings.getMethodInfo().addAttribute(annotationsAttribute);
        } catch(NotFoundException e) {
            logger.log(Level.SEVERE,e.getMessage(),e);
        }

        Syringe sockc = Syringe.getSyringe("com.wurmonline.communication.SocketConnection");
        if(Config.connectionFix) {
            sockc.instrument("tick","()V",new ExprEditor() {
                @Override
                public void edit(MethodCall m) throws CannotCompileException {
                    if(m.getClassName().equals("java.nio.channels.SocketChannel") && m.getMethodName().equals("read")) {
                        m.replace("{ int r = $proceed($$); if (r < 0) throw new java.io.IOException(\"Disconnected.\"); $_ = r; }");
                    }
                }
            });
        }
        /* World: */
        final Syringe world = Syringe.getSyringe("com.wurmonline.client.game.World");
        String setServerInformation = "ServerConnection.handShake($0);\n";
        if(Config.customMap)
            setServerInformation += "ServerConnection.setServerInformation($0,$1,$2,$3);\n";
        world.insertBefore("setServerInformation","{\n"+setServerInformation+"}",null);

        /* SimpleServerConnectionClass: */
        final Syringe sscc = Syringe.getSyringe("com.wurmonline.client.comm.SimpleServerConnectionClass");
        sscc.instrument("reallyHandle",new ExprEditor() {
            int i = 0;

            @Override
            public void edit(MethodCall mc) throws CannotCompileException {
                if(mc.getMethodName().equals("get")) {
                    if(i==0) {
                        mc.replace("$_ = $proceed($$);if($_==-101) {ServerConnection.handlePacket(bb);return;}");
                        logger.info("SimpleServerConnectionClass: Handle custom server communication.");
                    }
                    ++i;
                }
            }
        });

        /* LwjglClient: */
        final Syringe lwjglc = Syringe.getSyringe("com.wurmonline.client.LwjglClient");
        if(Config.useWindowedFullscreenSizeAndPosition) {
            lwjglc.insertBefore("getWindowedFullscreenSizeAndPosition","{\n"+
                                                                       "   java.awt.Rectangle r = ClientWindow.getWindowedFullscreenSizeAndPosition();\n"+
                                                                       "   if(r!=null) return r;\n"+
                                                                       "}",
                                "Modified window update for fixed sized borderless window");
        }

        /* TilePicker: */
        final Syringe tp = Syringe.getSyringe("com.wurmonline.client.renderer.TilePicker");
        tp.setBody("getHoverName","return Tiles.tilePickerName($0,$0.world,$0.x,$0.y,$0.section,$0.getDistance());",null);

        /* CaveWallPicker: */
        final Syringe cwp = Syringe.getSyringe("com.wurmonline.client.renderer.cave.CaveWallPicker");
        cwp.setBody("getHoverName","return Tiles.caveWallPickerName($0,$0.world,$0.x,$0.y,$0.name,$0.getDistance());",null);

        /* CreatureCellRenderable: */
        final Syringe ccr = Syringe.getSyringe("com.wurmonline.client.renderer.cell.CreatureCellRenderable");
        ccr.setBody("getHoverName","return Creatures.creatureCellRenderableName($0);",null);

        if(Config.autoSaveToolBelt) {
            /* ToolBeltComponent: */
            final Syringe tbc = Syringe.getSyringe("com.wurmonline.client.renderer.gui.ToolBeltComponent");
            tbc.insertAfter("itemDropped","this.toolBelt.saveArrangement(com.wurmonline.client.settings.PlayerData.lastToolBelt);",null);
        }
    }
}
