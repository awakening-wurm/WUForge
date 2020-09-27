package net.wurmunlimited.forge;

import java.util.HashMap;
import java.util.Map;

public class VDF {

    public static class VDFObject {
        public final VDFObject parent;
        public final String key;
        public final String value;
        public final Map<String,VDFObject> values;

        private VDFObject(VDFObject parent,String key,String value) {
            this.parent = parent;
            this.key = key;
            this.value = value;
            this.values = null;
        }

        private VDFObject(VDFObject parent,String key,Map<String,VDFObject> values) {
            this.parent = parent;
            this.key = key;
            this.value = null;
            this.values = values;
        }

        public boolean isArray() {
            return values!=null;
        }

        public VDFObject getValue(String key) {
            if(values!=null) return values.get(key);
            return null;
        }
    }

    public static class VDFException extends Exception {

        public VDFException(String message) {
            super(message);
        }
    }

    private int index;
    private String vdf;

    public VDF() {
    }

    public VDFObject parse(String vdf) throws VDFException {
        VDFObject object = new VDFObject(null,null,new HashMap<>());
        this.index = 0;
        this.vdf = vdf;
        parseArray(object,false);
        return object;
    }

    private void parseArray(VDFObject object,boolean includeBraces) throws VDFException {
        int i = index;
        if(includeBraces) ++index;
        while(true) {
            skipWhiteSpace();
            if(index>=vdf.length()) {
                if(includeBraces) break;
                else return;
            }
            if(includeBraces && vdf.charAt(index)=='}') {
                ++index;
                return;
            }
            String key = getString();
            skipWhiteSpace();
            char c = this.vdf.charAt(this.index);
            if(c=='"' || c=='\'') {
                String value = getString();
                object.values.put(key,new VDFObject(object,key,value));
            } else if(c=='{') {
                VDFObject o = new VDFObject(object,key,new HashMap<>());
                parseArray(o,true);
                object.values.put(key,o);
            } else {
                break;
            }
        }
        if(includeBraces) {
            throw new VDFException("Malformed value starting at "+i+".");
        }
    }

    private boolean skipWhiteSpace() {
        while(true) {
            if(index>=vdf.length()) return false;
            char c = vdf.charAt(index);
            if(c!=' ' && c!='\t' && c!='\n' && c!='\r') return true;
            ++index;
        }
    }

    private String getString() throws VDFException {
        int i = index;
        int e = 0;
        char q = vdf.charAt(index);
        if(q!='"' && q!='\'') throw new VDFException("Missing quote at expected string, at "+index+".");
        ++index;
        while(true) {
            if(index>=vdf.length()) throw new VDFException("String started at "+i+" has no ending quotation mark.");
            char c = vdf.charAt(index);
            if(c==q) break;
            if(c=='\\') {
                ++e;
                ++index;
            }
            ++index;
        }
        String str = vdf.substring(i+1,index);
        if(e>0) str = unescape(str);
        ++index;
        return str;
    }

    public static String unescape(String str) {
        if(str==null || str.isEmpty()) return str;
        StringBuilder sb = new StringBuilder(str.length());
        for(int i = 0; i<str.length(); ++i) {
            char c = str.charAt(i);
            if(c=='\\') {
                char nextChar = i==str.length()-1? '\\' : str.charAt(i+1);
                if(nextChar>='0' && nextChar<='7') {
                    String code = ""+nextChar;
                    i++;
                    char c1 = str.charAt(i+1);
                    if(i<str.length()-1 && c1>='0' && c1<='7') {
                        code += c1;
                        i++;
                        char c2 = str.charAt(i+1);
                        if((i<str.length()-1) && c2>='0' && c2<='7') {
                            code += c2;
                            i++;
                        }
                    }
                    sb.append((char)Integer.parseInt(code,8));
                    continue;
                }
                switch(nextChar) {
                    case '\\':c = '\\';break;
                    case 'b':c = '\b';break;
                    case 'f':c = '\f';break;
                    case 'n':c = '\n';break;
                    case 'r':c = '\r';break;
                    case 't':c = '\t';break;
                    case '\"':c = '\"';break;
                    case '\'':c = '\'';break;
                    // Hex Unicode: u????
                    case 'u':
                        if(i>=str.length()-5) {
                            c = 'u';
                            break;
                        }
                        int code = Integer.parseInt(str.substring(i+2,i+6),16);
                        sb.append(Character.toChars(code));
                        i += 5;
                        continue;
                }
                i++;
            }
            sb.append(c);
        }
        return sb.toString();
    }
}
