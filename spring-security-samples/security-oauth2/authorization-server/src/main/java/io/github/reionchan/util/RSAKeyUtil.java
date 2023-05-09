package io.github.reionchan.util;

import lombok.extern.apachecommons.CommonsLog;
import org.apache.tomcat.util.codec.binary.Base64;
import org.springframework.lang.Nullable;
import org.springframework.security.converter.RsaKeyConverters;
import org.springframework.util.Assert;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

/**
 * RSA 密钥处理工具类
 *
 * @author Reion
 * @date 2023-05-04
 **/
@CommonsLog
public final class RSAKeyUtil {

    private static final String DASHES = "-----";
    private static final String LINE_SEPARATOR = System.getProperty("line.separator");
    private static final String X509_PEM_HEADER = DASHES + "BEGIN PUBLIC KEY" + DASHES;
    private static final String X509_PEM_FOOTER = DASHES + "END PUBLIC KEY" + DASHES;
    private static final String PKCS8_PEM_HEADER = DASHES + "BEGIN PRIVATE KEY" + DASHES;
    private static final String PKCS8_PEM_FOOTER = DASHES + "END PRIVATE KEY" + DASHES;

    private static final String TEXTUAL_KEY_FILE_EXTENSION = ".pem";
    private static final String PUBLIC_KEY_PEM_NAME = "public" + TEXTUAL_KEY_FILE_EXTENSION;
    private static final String PRIVATE_KEY_PEM_NAME = "private" + TEXTUAL_KEY_FILE_EXTENSION;

    private static final String BINARY_KEY_FILE_EXTENSION = ".der";
    private static final String PUBLIC_KEY_DER_NAME = "public" + BINARY_KEY_FILE_EXTENSION;
    private static final String PRIVATE_KEY_DER_NAME = "private" + BINARY_KEY_FILE_EXTENSION;

    /**
     * @param keySize RSA 算法模数长度，最低 2048
     * @return 公私钥匙对
     */
    public static KeyPair generateKeyPair(@Nullable Integer keySize) {
        Integer size = 2048;
        if (keySize != null && keySize > 2048) {
            size = keySize;
        }
        log.info("Generate RSA key pair, using " + size + " key size.");
        KeyPairGenerator keyPairGenerator = null;
        try {
            keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            keyPairGenerator.initialize(2048);
            return keyPairGenerator.generateKeyPair();
        } catch (NoSuchAlgorithmException e) {
            log.error("Generate RSA key error!", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * 将 RSA 密钥对保存到指定路径的二进制文件
     * @param rootPath 保存目录
     * @param keyPair RSA 密钥对
     */
    public static void saveKeyPair2DerFilePath(Path rootPath, KeyPair keyPair) {
        Assert.notNull(rootPath, "rootPath is null");
        Assert.notNull(keyPair, "keyPair is null");

        Path pubPath = Paths.get(rootPath.toString(), "rsa", PUBLIC_KEY_DER_NAME);
        Path priPath = Paths.get(rootPath.toString(), "rsa", PRIVATE_KEY_DER_NAME);

        if (pubPath.toFile().exists() || priPath.toFile().exists()) {
            log.warn(String.format("%s or %s exist!", pubPath.toAbsolutePath(), priPath.toAbsolutePath()));
            return;
        }

        File parent = pubPath.getParent().toFile();
        if (!parent.exists()) {
            parent.mkdirs();
        }

        try (FileOutputStream fos = new FileOutputStream(pubPath.toFile())) {
            fos.write(keyPair.getPublic().getEncoded());
            log.info("save publicKey at:" + pubPath.toAbsolutePath());
        } catch (IOException e) {
            log.error("save public.key error!", e);
            if (pubPath.toFile().exists()) {
                pubPath.toFile().delete();
            }
        }

        try (FileOutputStream fos = new FileOutputStream(priPath.toFile())) {
            fos.write(keyPair.getPrivate().getEncoded());
            log.info("save privateKey at:" + priPath.toAbsolutePath());
        } catch (IOException e) {
            log.error("save private.key error!", e);
            if (priPath.toFile().exists()) {
                priPath.toFile().delete();
            }
        }
    }

    /**
     * 将 RSA 密钥对保存到指定路径的文本文件
     * @param rootPath 保存目录
     * @param keyPair RSA 密钥对
     */
    public static void saveKeyPair2PemFilePath(Path rootPath, KeyPair keyPair) {
        Assert.notNull(rootPath, "rootPath is null");
        Assert.notNull(keyPair, "keyPair is null");

        Path pubPath = Paths.get(rootPath.toString(), "rsa", PUBLIC_KEY_PEM_NAME);
        Path priPath = Paths.get(rootPath.toString(), "rsa", PRIVATE_KEY_PEM_NAME);

        if (pubPath.toFile().exists() || priPath.toFile().exists()) {
            log.warn(String.format("%s or %s exist!", pubPath.toAbsolutePath(), priPath.toAbsolutePath()));
            return;
        }

        File parent = pubPath.getParent().toFile();
        if (!parent.exists()) {
            parent.mkdirs();
        }

        try (FileOutputStream fos = new FileOutputStream(pubPath.toFile())) {
            fos.write(getPublicKeyAsX509Pem((RSAPublicKey) keyPair.getPublic()).getBytes(StandardCharsets.UTF_8));
            log.info("save publicKey at:" + pubPath.toAbsolutePath());
        } catch (IOException e) {
            log.error("save public.key error!", e);
            if (pubPath.toFile().exists()) {
                pubPath.toFile().delete();
            }
        }

        try (FileOutputStream fos = new FileOutputStream(priPath.toFile())) {
            fos.write(getPrivateKeyAsPKCS8Pem((RSAPrivateKey) keyPair.getPrivate()).getBytes(StandardCharsets.UTF_8));
            log.info("save privateKey at:" + priPath.toAbsolutePath());
        } catch (IOException e) {
            log.error("save private.key error!", e);
            if (priPath.toFile().exists()) {
                priPath.toFile().delete();
            }
        }
    }

    /**
     * 从指定的路径加载二进制的 RSA 密钥对
     *
     * @param rootPath 指定目录
     * @return
     */
    public static KeyPair loadKeyPairFromDerFilePath(Path rootPath) {
        Assert.notNull(rootPath, "rootPath is null");

        Path pubPath = Paths.get(rootPath.toString(), "rsa", PUBLIC_KEY_DER_NAME);
        Path priPath = Paths.get(rootPath.toString(), "rsa", PRIVATE_KEY_DER_NAME);

        if (!pubPath.toFile().exists() || !priPath.toFile().exists()) {
            throw new RuntimeException(String.format("%s or %s does not exist!", pubPath.toAbsolutePath(), priPath.toAbsolutePath()));
        }

        byte[] pubBytes, priBytes;

        try (FileInputStream fis = new FileInputStream(pubPath.toFile())) {
            log.info("load public key from path:" + pubPath.toAbsolutePath());
            pubBytes = fis.readAllBytes();
        } catch (IOException e) {
            throw new RuntimeException("load public key error!", e);
        }

        try (FileInputStream fis = new FileInputStream(priPath.toFile())) {
            log.info("load private key from path:" + priPath.toAbsolutePath());
            priBytes = fis.readAllBytes();
        } catch (IOException e) {
            throw new RuntimeException("load public key error!", e);
        }

        Assert.notNull(pubBytes, "public key bytes are null");
        Assert.notNull(priBytes, "private key bytes are null");

        try {
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            RSAPublicKey pubKey = (RSAPublicKey) keyFactory.generatePublic(new X509EncodedKeySpec(pubBytes));
            RSAPrivateCrtKey priKey = (RSAPrivateCrtKey) keyFactory.generatePrivate(new PKCS8EncodedKeySpec(priBytes));
            return new KeyPair(pubKey, priKey);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new RuntimeException("Load RSA key error!", e);
        }
    }

    /**
     * 从指定的路径加载文本型的 RSA 密钥对
     *
     * @param rootPath 指定目录
     * @return
     */
    public static KeyPair loadKeyPairFromPemFilePath(Path rootPath) {
        Assert.notNull(rootPath, "rootPath is null");

        Path pubPath = Paths.get(rootPath.toString(), "rsa", PUBLIC_KEY_PEM_NAME);
        Path priPath = Paths.get(rootPath.toString(), "rsa", PRIVATE_KEY_PEM_NAME);

        if (!pubPath.toFile().exists() || !priPath.toFile().exists()) {
            throw new RuntimeException(String.format("%s or %s does not exist!", pubPath.toAbsolutePath(), priPath.toAbsolutePath()));
        }

        RSAPublicKey pubKey;
        RSAPrivateKey priKey;

        try (FileInputStream fis = new FileInputStream(pubPath.toFile())) {
            log.info("load public key from path:" + pubPath.toAbsolutePath());
            pubKey = RsaKeyConverters.x509().convert(fis);
        } catch (IOException e) {
            throw new RuntimeException("load public key error!", e);
        }

        try (FileInputStream fis = new FileInputStream(priPath.toFile())) {
            log.info("load private key from path:" + priPath.toAbsolutePath());
            priKey = RsaKeyConverters.pkcs8().convert(fis);
        } catch (IOException e) {
            throw new RuntimeException("load public key error!", e);
        }

        Assert.notNull(pubKey, "public key is null");
        Assert.notNull(priKey, "private key is null");

        return new KeyPair(pubKey, priKey);
    }

    /**
     * 从指定目录加载二进制的密钥对，没有就创建并保存
     * @param rootPath 密钥对存放根目录
     * @param keySize 生成密钥的模数长度
     * @return
     */
    public static KeyPair loadOrCreateThenSaveByDerFile(Path rootPath, @Nullable Integer keySize) {
        Assert.notNull(rootPath, "rootPath is null");
        KeyPair keyPair = null;
        try {
            keyPair = loadKeyPairFromDerFilePath(rootPath);
        } catch (RuntimeException e) {
            log.warn("Can not load keyPair, try to generate it.");
        }
        if (keyPair != null) {
            log.info("KeyPair has been loaded!");
            return keyPair;
        }
        keyPair = generateKeyPair(keySize);
        log.info("Generate new keyPair!");
        saveKeyPair2DerFilePath(rootPath, keyPair);
        log.info("New keyPair has been saved!");
        return keyPair;
    }

    /**
     * 从指定目录加载文本格式的密钥对，没有就创建并保存
     * @param rootPath 密钥对存放根目录
     * @param keySize 生成密钥的模数长度
     * @return
     */
    public static KeyPair loadOrCreateThenSaveByPemFile(Path rootPath, @Nullable Integer keySize) {
        Assert.notNull(rootPath, "rootPath is null");
        KeyPair keyPair = null;
        try {
            keyPair = loadKeyPairFromPemFilePath(rootPath);
        } catch (RuntimeException e) {
            log.warn("Can not load keyPair, try to generate it.");
        }
        if (keyPair != null) {
            log.info("KeyPair has been loaded!");
            return keyPair;
        }
        keyPair = generateKeyPair(keySize);
        log.info("Generate new keyPair!");
        saveKeyPair2PemFilePath(rootPath, keyPair);
        log.info("New keyPair has been saved!");
        return keyPair;
    }

    /**
     * 将公钥生成 X509 PEM 字符串
     * @param publicKey 公钥
     * @return
     */
    public static String getPublicKeyAsX509Pem(RSAPublicKey publicKey) {
        Assert.notNull(publicKey, "publicKey is null");
        String pubBaseStr = Base64.encodeBase64String(publicKey.getEncoded());
        StringBuilder stringBuilder = new StringBuilder(X509_PEM_HEADER).append(LINE_SEPARATOR);
        for (int i=0; i<pubBaseStr.length(); i++) {
            if (i!=0 && i%64==0) {
                stringBuilder.append(LINE_SEPARATOR);
            }
            stringBuilder.append(pubBaseStr.charAt(i));
            if (i == pubBaseStr.length()-1) {
                stringBuilder.append(LINE_SEPARATOR);
            }
        }
        stringBuilder.append(X509_PEM_FOOTER);
        return stringBuilder.toString();
    }

    /**
     * 将公钥生成 X509 PEM 字符串
     * @param privateKey 公钥
     * @return
     */
    public static String getPrivateKeyAsPKCS8Pem(RSAPrivateKey privateKey) {
        Assert.notNull(privateKey, "privateKey is null");
        String pubBaseStr = Base64.encodeBase64String(privateKey.getEncoded());
        StringBuilder stringBuilder = new StringBuilder(PKCS8_PEM_HEADER).append(LINE_SEPARATOR);
        for (int i=0; i<pubBaseStr.length(); i++) {
            if (i!=0 && i%64==0) {
                stringBuilder.append(LINE_SEPARATOR);
            }
            stringBuilder.append(pubBaseStr.charAt(i));
            if (i == pubBaseStr.length()-1) {
                stringBuilder.append(LINE_SEPARATOR);
            }
        }
        stringBuilder.append(PKCS8_PEM_FOOTER);
        return stringBuilder.toString();
    }
}
