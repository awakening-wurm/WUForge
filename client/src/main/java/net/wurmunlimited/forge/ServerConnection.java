package net.wurmunlimited.forge;

import com.wurmonline.client.comm.SimpleServerConnectionClass;
import com.wurmonline.client.game.World;
import com.wurmonline.client.renderer.gui.WorldMap;
import com.wurmonline.client.renderer.gui.maps.ClusterMap;
import com.wurmonline.communication.SocketConnection;
import net.wurmunlimited.forge.VersionHandler.ModInfo;
import net.wurmunlimited.forge.VersionHandler.ReleaseVersion;
import net.wurmunlimited.forge.config.ForgeClientConfig;
import net.wurmunlimited.forge.interfaces.ConnectionHandler;
import net.wurmunlimited.forge.util.ClientChecksum;
import org.gotti.wurmunlimited.modcomm.PacketWriter;
import org.gotti.wurmunlimited.modloader.ReflectionUtil;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;


public class ServerConnection implements ConnectionHandler {

    private static final Logger logger = Logger.getLogger(ServerConnection.class.getName());

    private class ByteArrayClassLoader extends ClassLoader {
        private byte[] data;

        public ByteArrayClassLoader(byte[] data) {
            this.data = data;
        }

        @Override
        public Class<?> findClass(String name) throws ClassNotFoundException {
            if(data!=null) return defineClass(name,data,0,data.length,null);
            return super.findClass(name);
        }
    }

    private static ServerConnection instance = null;

    public static ServerConnection getInstance() {
        if(instance==null) instance = new ServerConnection();
        return instance;
    }

    @SuppressWarnings("unused")
    public static void handlePacket(final ByteBuffer msg) {
        try {
            final byte type = msg.get();
            switch(type) {
                case 1:
                    getInstance().sendInstalledMods(msg);
                    break;

                default:
                    logger.warning("Unknown packet from server ("+type+")");
                    break;
            }
        } catch(Exception e) {
            logger.log(Level.WARNING,"Error handling packet from server: "+e.getMessage(),e);
        }
    }

    @SuppressWarnings("unused")
    public static void handShake(final World world) {
        logger.info("Starting handshake");
        getInstance().world = world;
        getInstance().sendHandshake();
    }

    @SuppressWarnings("unused")
    public static void setServerInformation(final World world,final int cluster,final boolean isEpic,final String serverName) {
        try {
            final String textureName = "map."+serverName.toLowerCase(Locale.ROOT);
            WorldMap worldMap = world.getHud().getWorldMap();
            ClusterMap currentCluster = ReflectionUtil.getPrivateField(worldMap,ReflectionUtil.getField(WorldMap.class,"currentCluster"));
            List<com.wurmonline.client.renderer.gui.maps.Map> maps = ReflectionUtil.getPrivateField(currentCluster,ReflectionUtil.getField(ClusterMap.class,"serverMaps"));
            for(com.wurmonline.client.renderer.gui.maps.Map map : maps)
                if(map.getTextureName().equalsIgnoreCase(textureName))
                    return;
            com.wurmonline.client.renderer.gui.maps.Map map = new com.wurmonline.client.renderer.gui.maps.Map(serverName,textureName,false,920,620);
            maps.add(map);
        } catch(Exception e) {
            logger.log(Level.WARNING,e.getMessage(),e);
        }
    }

    private World world;
    private Field serverConnectionField;

    private ServerConnection() {
        world = null;
        serverConnectionField = null;
    }

    public void init() {
        ForgeClientConfig config = ForgeClientConfig.getInstance();
        VersionHandler.getInstance().getInstalledMods();
        VersionHandler.getInstance().loadNewVersions();
        Map<ModInfo,ReleaseVersion> updates = VersionHandler.getInstance().getUpdates();
        if(!updates.isEmpty() && config.autoUpdate) {
            String jarName = "forge.jar";
            Path jar = config.getForgeDir().resolve(jarName).toAbsolutePath();
            logger.info("Starting the auto installer ("+jar.toString()+")...");
            try {
                String exec = "java -jar "+jarName;
                Process p = Runtime.getRuntime().exec(exec,null,config.getForgeDir().toFile());
            } catch(IOException e) {
                logger.log(Level.SEVERE,"Could not start auto installer process: "+e.getMessage(),e);
            }
            System.exit(0);
        }
    }

    private SocketConnection getServerConnection() {
        try {
            SimpleServerConnectionClass serverConnection = world.getServerConnection();
            if(serverConnectionField==null) {
                serverConnectionField = SimpleServerConnectionClass.class.getDeclaredField("connection");
                serverConnectionField.setAccessible(true);
            }
            return (SocketConnection)serverConnectionField.get(serverConnection);
        } catch(NoSuchFieldException|IllegalAccessException var1) {
            throw new RuntimeException(var1);
        }
    }

    private void sendInstalledMods(final ByteBuffer msg) {
        logger.info("Sending client mods:");
        try {
            Map<String,ModInfo> installedMods = VersionHandler.getInstance().getInstalledMods();
            try(final PacketWriter writer = new PacketWriter()) {
                writer.writeByte(-101);
                writer.writeByte(2);
                writer.writeByte(1);
                writer.writeInt(installedMods.size());
                Collection<ModInfo> mods = installedMods.values();
                for(ModInfo modInfo : mods) {
                    logger.info("Client mod: "+modInfo.getName()+", hash: "+modInfo.getHash());
                    writer.writeUTF(modInfo.getName());
                    writer.writeUTF(modInfo.getHash());
                }
                writer.writeUTF(ClientChecksum.getChecksum(mods));
                sendPacket(writer);
            }
        } catch(IOException e) {
            logger.log(Level.SEVERE,"Error sending mods: "+e.getMessage(),e);
        }
    }

    @Override
    public void sendPacket(final PacketWriter writer) {
        try {
            final SocketConnection conn = getServerConnection();
            final ByteBuffer buff = conn.getBuffer();
            buff.put(writer.getBytes());
            conn.flush();
        } catch(IOException e) {
            logger.log(Level.SEVERE,"Error sending package: "+e.getMessage(),e);
        }
    }

    private void sendHandshake() {
        logger.info("Sending handshake...");
        try(final PacketWriter writer = new PacketWriter()) {
            writer.writeByte(-101);
            writer.writeByte(1);
            sendPacket(writer);
        } catch(IOException e) {
            logger.log(Level.SEVERE,"Error sending handshake: "+e.getMessage(),e);
        }
    }
}
