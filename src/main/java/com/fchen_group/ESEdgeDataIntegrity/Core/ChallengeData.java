package com.fchen_group.ESEdgeDataIntegrity.Core;

/*
*
* */
public class ChallengeData {

    public int[] blockIndex;
    public byte[] matrixA;
    public byte[] coefficient;
    public byte[] fkMatrix;// this is secret;

    public ChallengeData(int[] blockIndex,byte[] matrixA,byte[]coefficient,byte[] fkMatrix){
        this.blockIndex = blockIndex;
        this.matrixA = matrixA;
        this.coefficient=coefficient;
        this.fkMatrix = fkMatrix;
    }

}
