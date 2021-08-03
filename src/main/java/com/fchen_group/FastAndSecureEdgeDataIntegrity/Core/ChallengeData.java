package com.fchen_group.FastAndSecureEdgeDataIntegrity.Core;

/*
*
* */
public class ChallengeData {

    public int[] blockIndex;
    public byte[] matrixA;

    public ChallengeData(int[] blockIndex,byte[] matrixA){
        this.blockIndex = blockIndex;
        this.matrixA = matrixA;
    }

}
