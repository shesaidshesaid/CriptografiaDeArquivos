package com.example.criptografiadearquivos.utils;

import java.io.InputStream;
import java.io.OutputStream;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.crypto.spec.PBEKeySpec;

public class CryptoUtils {

    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int IV_LENGTH_BYTE = 12;
    private static final int TAG_LENGTH_BIT = 128;
    private static final int KEY_LENGTH_BIT = 256;
    private static final int PBKDF2_ITERATIONS = 65536;

    // Método para criptografar um arquivo usando streams
    public static void encryptFile(InputStream inputStream, OutputStream outputStream, String password) throws Exception {
        // Gera a chave de criptografia
        SecretKeySpec key = deriveKeyFromPassword(password);

        // Gera um IV aleatório
        byte[] iv = new byte[IV_LENGTH_BYTE];
        SecureRandom random = new SecureRandom();
        random.nextBytes(iv);

        // Configura o Cipher
        GCMParameterSpec gcmSpec = new GCMParameterSpec(TAG_LENGTH_BIT, iv);
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        cipher.init(Cipher.ENCRYPT_MODE, key, gcmSpec);

        // Escreve o IV no início do arquivo de saída
        outputStream.write(iv);

        // Criptografa o conteúdo
        try (CipherOutputStream cipherOutputStream = new CipherOutputStream(outputStream, cipher)) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                cipherOutputStream.write(buffer, 0, bytesRead);
            }
        }
        inputStream.close();
    }

    // Método para descriptografar um arquivo usando streams
    public static void decryptFile(InputStream inputStream, OutputStream outputStream, String password) throws Exception {
        // Lê o IV do arquivo
        byte[] iv = new byte[IV_LENGTH_BYTE];
        int ivRead = inputStream.read(iv);
        if (ivRead != IV_LENGTH_BYTE) {
            throw new IllegalArgumentException("IV inválido ou corrompido.");
        }

        // Configura o Cipher
        GCMParameterSpec gcmSpec = new GCMParameterSpec(TAG_LENGTH_BIT, iv);
        SecretKeySpec key = deriveKeyFromPassword(password);
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        cipher.init(Cipher.DECRYPT_MODE, key, gcmSpec);

        // Descriptografa o conteúdo
        try (CipherInputStream cipherInputStream = new CipherInputStream(inputStream, cipher)) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = cipherInputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
        } catch (Exception e) {
            throw new SecurityException("Erro na descriptografia: senha incorreta ou arquivo corrompido.", e);
        }
        inputStream.close();
        outputStream.close();
    }

    // Método para derivar uma chave AES forte a partir de uma senha usando PBKDF2
    private static SecretKeySpec deriveKeyFromPassword(String password) throws Exception {
        byte[] salt = "FixedSaltForDemoPurposes".getBytes(); // Substitua por um gerador de sal dinâmico em produção
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, PBKDF2_ITERATIONS, KEY_LENGTH_BIT);
        SecretKey tmp = factory.generateSecret(spec);
        return new SecretKeySpec(tmp.getEncoded(), ALGORITHM);
    }
}
