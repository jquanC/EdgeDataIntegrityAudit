package com.fchen_group.FastAndSecureEdgeDataIntegrity.Core;

public class Key {
    private String keyPRF; //as sk for PRF
    private String keyMatrix;

    public Key(String KeyPRF,String keyMatrix){
        this.keyPRF = KeyPRF;
        this.keyMatrix = keyMatrix;
    }
    public String getKeyPRF(){
        return  keyPRF;
    }
    public String getKeyMatrix(){
        return keyMatrix;
    }

}
