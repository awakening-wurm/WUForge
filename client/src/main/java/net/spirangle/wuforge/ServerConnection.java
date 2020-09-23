package net.spirangle.wuforge;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.ParseException;
import com.wurmonline.client.comm.SimpleServerConnectionClass;
import com.wurmonline.client.game.World;
import com.wurmonline.client.renderer.gui.WorldMap;
import com.wurmonline.client.renderer.gui.maps.ClusterMap;
import com.wurmonline.communication.SocketConnection;
import net.spirangle.wuforge.util.ClientChecksum;
import net.spirangle.wuforge.util.HttpClient;
import org.gotti.wurmunlimited.modcomm.PacketWriter;
import org.gotti.wurmunlimited.modloader.ReflectionUtil;

import javax.xml.bind.DatatypeConverter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.file.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;


public class ServerConnection {

    private static final Logger logger = Logger.getLogger(ServerConnection.class.getName());

    private static final String BASE_URL = "https://awakening.spirangle.net/";

    public static class ClientMod {
        String url;
        String hash;

        ClientMod(String url,String hash) {
            this.url = url;
            this.hash = hash;
        }
    }

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

    public static void handShake(final World world) {
        logger.info("Starting handshake");
        getInstance().world = world;
        getInstance().sendHandshake();
    }

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

    private String modsDir;
    private Map<String,ClientMod> availableMods;
    private World world;
    private List<HashMap<String,Object>> installedMods;
    private Map<String,Path> installMods;
    private Field serverConnectionField;

    private ServerConnection() {
        modsDir = null;
        availableMods = new HashMap<>();
        world = null;
        installedMods = null;
        installMods = new HashMap<>();
        serverConnectionField = null;
    }

    public void getAvailableMods(String modsDir) {
        this.modsDir = modsDir;
        try {
            HttpClient http = new HttpClient();
            http.request(BASE_URL+"mods/check");
            if(http.getStatus()==200) {
                try {
                    JsonObject jo = Json.parse(http.getResponse()).asObject();
                    String version = jo.getString("version",null);
                    logger.info("Receiving available mods:");
                    JsonObject mods = jo.get("mods").asObject();
                    for(JsonObject.Member entry : mods) {
                        String name = entry.getName();
                        JsonObject value = entry.getValue().asObject();
                        String url = BASE_URL+"mods/"+value.getString("url","");
                        String hash = value.getString("hash","");
                        ClientMod cm = new ClientMod(url,hash);
                        availableMods.put(name,cm);
                        logger.info("Client mod: "+name+", md5: "+hash);
                    }
                } catch(ParseException e) {
                    logger.log(Level.WARNING,"getAvailableMods: "+e.getMessage(),e);
                }
            }
        } catch(Exception e) {
            logger.log(Level.WARNING,"getAvailableMods: "+e.getMessage(),e);
        }
    }

    public void init() {
        try {
            getInstalledMods(Paths.get(modsDir));
            runAutoInstaller();
        } catch(IOException|NoSuchAlgorithmException e) {
            throw new RuntimeException("Error loading mod info: "+e.getMessage(),e);
        }
    }

    public boolean install() {
        try {
            String path = new File(AutoInstaller.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getParentFile().getParentFile().getAbsolutePath();
            logger.info("Install to dir: "+path);
            getInstalledMods(Paths.get(path,new String[0]));
            return installMods();
        } catch(IOException|NoSuchAlgorithmException|URISyntaxException e) {
            logger.log(Level.SEVERE,"Error loading mod info: "+e.getMessage(),e);
        }
        return false;
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

    private void sendPacket(final PacketWriter writer) {
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

    private void sendInstalledMods(final ByteBuffer msg) {
        logger.info("Sending client mods:");
        try(final PacketWriter writer = new PacketWriter()) {
            writer.writeByte(-101);
            writer.writeByte(2);
            writer.writeByte(1);
            writer.writeInt(installedMods.size());
            for(final HashMap<String,Object> mod : installedMods) {
                String name = (String)mod.get("name");
                String hash = (String)mod.get("hash");
                logger.info("Client mod: "+name+", hash: "+hash);
                writer.writeUTF(name);
                writer.writeUTF(hash);
            }
            writer.writeUTF(ClientChecksum.getChecksum(modsDir,installedMods));
            sendPacket(writer);
        } catch(IOException e) {
            logger.log(Level.SEVERE,"Error sending mods: "+e.getMessage(),e);
        }
    }

    private List<HashMap<String,Object>> getInstalledMods(final Path modDir) throws IOException, NoSuchAlgorithmException {
        if(this.installedMods==null) {
            final List<HashMap<String,Object>> mods = new LinkedList<>();
            try(final DirectoryStream<Path> directoryStream = Files.newDirectoryStream(modDir,"*.jar")) {
                for(final Path modJar : directoryStream) {
                    final String modName = modJar.getFileName().toString().replaceAll("\\.jar$","");
                    final HashMap<String,Object> mod = getModInfo(modName,modJar,getSha1Sum(modJar),ClientChecksum.getMD5(modJar));
                    mods.add(mod);
                }
            }
            this.installedMods = mods;
            logger.info("Located installed client mods:");
            installMods = new HashMap<>();
            for(final HashMap<String,Object> mod : mods) {
                String name = (String)mod.get("name");
                String md5 = (String)mod.get("md5");
                logger.info("Client mod: "+name+", md5: "+md5);
                ClientMod cm = availableMods.get(name);
                if(cm!=null && !cm.hash.equalsIgnoreCase(md5)) {
                    logger.info("Newer version ready to be installed... "+cm.hash);
                    Path jar = (Path)mod.get("jar");
                    installMods.put(cm.url,jar);
                }
            }
        }
        return this.installedMods;
    }

    private void runAutoInstaller() {
        /*String modName = "awakening";
        Path path = Paths.get(modsDir);
        Path temp = path.resolve(modName).resolve("~"+modName+".jar").toAbsolutePath();
        try {
            Files.deleteIfExists(temp);
        } catch(IOException e) {}
        if(!installMods.isEmpty() && Config.autoUpdate) {
            Path jar = path.resolve(modName).resolve(modName+".jar").toAbsolutePath();
            try {
                File dir = new File(jar.toString()).getParentFile();
                Files.copy(jar,temp,StandardCopyOption.REPLACE_EXISTING);
                logger.info("Starting the auto installer ("+dir.toString()+"/~awakening.jar)...");
                Process p = Runtime.getRuntime().exec("java -jar ~awakening.jar \""+modsDir+"\"",null,dir);
            } catch(IOException e) {
                logger.log(Level.SEVERE,"Could not start auto installer process: "+e.getMessage(),e);
            }
            System.exit(0);
        }*/
    }

    private boolean installMods() {
        if(!installMods.isEmpty()) {
            Iterator<Map.Entry<String,Path>> it = installMods.entrySet().iterator();
            int num = 0;
            while(it.hasNext()) {
                Map.Entry<String,Path> entry = it.next();
                String url = entry.getKey();
                Path jar = entry.getValue();
                logger.info("Downloading and installing "+url);
                if(HttpClient.download(url,jar)) ++num;
            }
            logger.info("Client mods have been updated, client needs to be restarted...");
            return num>0;
        }
        return false;
    }

    private HashMap<String,Object> getModInfo(String name,Path jar,String hash,String md5) {
        HashMap<String,Object> mod = new HashMap<>();
        mod.put("name",name);
        mod.put("jar",jar);
        mod.put("hash",hash);
        mod.put("md5",md5);
        return mod;
    }

    private String getSha1Sum(final Path path) throws IOException, NoSuchAlgorithmException {
        try(final InputStream is = Files.newInputStream(path,new OpenOption[0])) {
            return this.getSha1Sum(is);
        }
    }

    private String getSha1Sum(final InputStream is) throws IOException, NoSuchAlgorithmException {
        final MessageDigest messageDigest = MessageDigest.getInstance("SHA-1");
        messageDigest.reset();
        int n = 0;
        final byte[] buffer = new byte[8192];
        while(n!=-1) {
            n = is.read(buffer);
            if(n>0) messageDigest.update(buffer,0,n);
        }
        final byte[] digest = messageDigest.digest();
        return DatatypeConverter.printHexBinary(digest);
    }
}
