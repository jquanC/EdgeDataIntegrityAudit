package com.fchen_group.PDPeEdgeDataIntegrity.Core;

import java.math.BigInteger;

public class Key {
    private String keyPRF; //as sk for PRF
    private BigInteger g;  //PublicExponent of public key



    private BigInteger N; // modulus

    public Key(String KeyPRF , BigInteger g , BigInteger N){
        this.keyPRF = KeyPRF;
        this.g = g ;
        this.N = N ;


    }
    public String getKeyPRF(){
        return  keyPRF;
    }
    public BigInteger getG() {
        return g;
    }

    public BigInteger getN() {
        return N;
    }
}
