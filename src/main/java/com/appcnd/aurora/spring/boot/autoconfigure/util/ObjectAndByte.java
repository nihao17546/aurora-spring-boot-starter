package com.appcnd.aurora.spring.boot.autoconfigure.util;

import java.io.*;

/**
 * @author nihao 2019/4/29
 */
public class ObjectAndByte {

    public final static byte[] toByteArray (Object obj) {
        ByteArrayOutputStream bos = null;
        ObjectOutputStream oos = null;
        try {
            bos = new ByteArrayOutputStream();
            oos = new ObjectOutputStream(bos);
            oos.writeObject(obj);
            oos.flush();
            return bos.toByteArray ();
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        } finally {
            if (oos != null) {
                try {
                    oos.close();
                } catch (IOException e) {
                }
            }
            if (bos != null) {
                try {
                    bos.close();
                } catch (IOException e) {
                }
            }
        }
    }

    public final static Object toObject (byte[] bytes) {
        ByteArrayInputStream bis = null;
        ObjectInputStream ois = null;
        try {
            bis = new ByteArrayInputStream (bytes);
            ois = new ObjectInputStream (bis);
            return ois.readObject();
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        } finally {
            if (ois != null) {
                try {
                    ois.close();
                } catch (IOException e) {
                }
            }
            if (bis != null) {
                try {
                    bis.close();
                } catch (IOException e) {
                }
            }
        }
    }
}
