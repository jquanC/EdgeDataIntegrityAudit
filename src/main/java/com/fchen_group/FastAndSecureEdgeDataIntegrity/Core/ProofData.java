package com.fchen_group.FastAndSecureEdgeDataIntegrity.Core;

public class ProofData {
    public int esID;
    public byte[] combineTag;
    public ProofData(int esId, byte[] combineTag){
        this.esID = esId;
        this.combineTag = combineTag;
    }
}
