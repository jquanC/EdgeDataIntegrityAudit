package com.fchen_group.ESEdgeDataIntegrity.Core;

public class Key {
    private String keyPRF; //as sk for PRF
    private String keyMatrix;
    private String coe;
    private String fk;

    public Key(String KeyPRF,String keyMatrix,String coe,String fk){
        this.keyPRF = KeyPRF;
        this.keyMatrix = keyMatrix;
        this.coe = coe;
        this.fk = fk;
    }
    public String getKeyPRF(){
        return  keyPRF;
    }
    public String getKeyMatrix(){
        return keyMatrix;
    }
    public String getCoe(){return coe;}
    public String getFk(){return fk;}


}
