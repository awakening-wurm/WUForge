package net.wurmunlimited.forge;

import org.junit.Assert;
import org.junit.Test;

public class VDFTest {

    @Test
    public void testParsingVDF() {
        VDF.VDFObject data = null;
        try {
            VDF vdf = new VDF();
            data = vdf.parse("\"Test\" \n"+
                             " {\n"+
                             "   \"Key1\"    \t\"Value1\"\n"+
                             "\t\"Key2\"\n"+
                             "   {\n"+
                             "      \"Key3\"   \"Value\\n2\\nwith escaped\\ttext.\"\n"+
                             "   }\n"+
                             "}\n"+
                             "");
        } catch(VDF.VDFException e) {}

        Assert.assertTrue("Parsed data isn't null.",data!=null);
        VDF.VDFObject o1 = data.getValue("Test");
        Assert.assertTrue("Value for \"Test\" isn't null.",o1!=null);
        VDF.VDFObject o2 = o1.getValue("Key1");
        Assert.assertTrue("Value for \"Key1\" isn't null.",o2!=null);
        Assert.assertTrue("Value for \"Key1\" equals \"Value1\".","Value1".equals(o2.value));
        VDF.VDFObject o3 = o1.getValue("Key2");
        Assert.assertTrue("Value for \"Key2\" isn't null and is an array.",o3!=null && o3.isArray());
        VDF.VDFObject o4 = o3.getValue("Key3");
        Assert.assertTrue("Value for \"Key3\" isn't null and is not an array.",o4!=null && !o4.isArray());
    }
}
