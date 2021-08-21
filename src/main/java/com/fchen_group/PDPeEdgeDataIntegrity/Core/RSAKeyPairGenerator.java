package com.fchen_group.PDPeEdgeDataIntegrity.Core;

import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

public class RSAKeyPairGenerator {
    private RSAPrivateKey privateKey;
    private RSAPublicKey publicKey;
    private BigInteger field;
/**
 *  keyGen.initialize(3072) : insure 128 bit secure level
 *  in this case, the public modulus length is 385 byte,the sk length is 384 byte , each  encryption _process result
 *  is not longer than  384 B
 *  we set BLOCK_SIZE = 384 B
 * */
    public RSAKeyPairGenerator() throws NoSuchAlgorithmException {

        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(3072); // insure 128 bit secure level
        KeyPair pair = keyGen.generateKeyPair();
        this.privateKey = (RSAPrivateKey) pair.getPrivate();
        this.publicKey = (RSAPublicKey) pair.getPublic();



    }

    public RSAPrivateKey getPrivateKey() {
        return privateKey;
    }

    public RSAPublicKey getPublicKey() {
        return publicKey;
    }




}
