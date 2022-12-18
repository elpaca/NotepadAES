package pers.zhengxiao.notepad;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class AESUtils {
    private static final byte[] defaultIV="0123456789abcdef".getBytes();

    /**
     * 加盐，UTF-8编码，SHA-256哈希方式，将文本转换为AES可用的key
     * @param prePassword 待转换文本密码
     * @return 256位密码
     */
    public static byte[] passwordToKeyWithSalt(String prePassword) throws NoSuchAlgorithmException{
        MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
        return messageDigest.digest((prePassword+"salt666233").getBytes(StandardCharsets.UTF_8));
    }

    /**
     *
     * @param data 明文
     * @param password 密码
     * @param iv CBC模式初始向量IV，若为{@code null}则使用默认IV
     * @return 密文
     */
    public static byte[] encrypt(byte[] data, String password, byte[] iv) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException, InvalidAlgorithmParameterException {
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(passwordToKeyWithSalt(password), "AES"), new IvParameterSpec(iv==null?defaultIV:iv));
        return cipher.doFinal(data);
    }

    /**
     *
     * @param data 密文
     * @param password 密码
     * @param iv CBC模式初始向量IV，若为{@code null}则使用默认IV
     * @return 明文
     */
    public static byte[] decrypt(byte[] data, String password, byte[] iv) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException, InvalidAlgorithmParameterException {
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(passwordToKeyWithSalt(password), "AES"), new IvParameterSpec(iv==null?defaultIV:iv));
        return cipher.doFinal(data);
    }
}
