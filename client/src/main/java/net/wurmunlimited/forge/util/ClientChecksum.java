package net.wurmunlimited.forge.util;

import net.wurmunlimited.forge.Config;

import javax.xml.bind.DatatypeConverter;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.List;


public class ClientChecksum {

    public static String getChecksum(List<HashMap<String,Object>> mods) {
        StringBuffer hashes = new StringBuffer();
        for(HashMap<String,Object> mod : mods) {
            String name = (String)mod.get("name");
            String hash = (String)mod.get("hash");
            if(name.equals("awakening")) {
                String hash2 = getAwakeningModHash();
                if(!hash2.equals(hash)) continue;
            }
            hashes.append(hash);
        }
        String checksum;
        try {
            checksum = getHex(encode(getHash(hashes.toString(),"MD5")));
        } catch(IOException|NoSuchAlgorithmException e) {
            checksum = ".";
        }
        return checksum;
    }

    public static String getMD5(final Path path) {
        try {
            byte[] hash = getHash(path,"MD5");
            return getHex(hash);
        } catch(IOException|NoSuchAlgorithmException e) {
            return null;
        }
    }

    private static String getAwakeningModHash() {
        final String name = "awakening";
        final Path jar = Config.modsLibDir.resolve(name+".jar");
        try {
            return getHex(getHash(jar,"SHA-1"));
        } catch(IOException|NoSuchAlgorithmException e) {}
        return null;
    }

    private static String getHex(final byte[] bytes) {
        return DatatypeConverter.printHexBinary(bytes);
    }

    private static byte[] getHash(final Path path,String algorithm) throws IOException, NoSuchAlgorithmException {
        try(final InputStream is = Files.newInputStream(path,new OpenOption[0])) {
            return getHash(is,algorithm);
        }
    }

    private static byte[] getHash(final String str,String algorithm) throws IOException, NoSuchAlgorithmException {
        final InputStream is = new ByteArrayInputStream(str.getBytes(Charset.forName("UTF-8")));
        return getHash(is,algorithm);
    }

    private static byte[] getHash(final InputStream is,String algorithm) throws IOException, NoSuchAlgorithmException {
        final MessageDigest messageDigest = MessageDigest.getInstance(algorithm);
        messageDigest.reset();
        int n = 0;
        final byte[] buffer = new byte[8192];
        while(n!=-1) {
            n = is.read(buffer);
            if(n>0) messageDigest.update(buffer,0,n);
        }
        return messageDigest.digest();
    }

    private static byte[] encode(final byte[] bytes) {
        long h = 0x520CD1372FEA884Fl;
        for(int i = 0; i<bytes.length; ++i) {
            int n = (int)bytes[i];
            int c = (int)(h >> ((i&3)*8))&0xFF;
            bytes[i] = (byte)((n^c)&0xFF);
        }
        return bytes;
    }
}
