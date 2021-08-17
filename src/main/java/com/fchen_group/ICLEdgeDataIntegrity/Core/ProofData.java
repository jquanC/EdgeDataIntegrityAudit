package com.fchen_group.ICLEdgeDataIntegrity.Core;

import java.math.BigInteger;

public class ProofData {
    public int esID;
    public BigInteger combineTag;
    public ProofData(int esId, BigInteger combineTag){
        this.esID = esId;
        this.combineTag = combineTag;
    }
}
