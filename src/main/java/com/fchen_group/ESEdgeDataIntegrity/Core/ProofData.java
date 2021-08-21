package com.fchen_group.ESEdgeDataIntegrity.Core;

public class ProofData {
    public int esID;
    public byte[] combineTag;
    public byte[] combineData;
    public ProofData(int esId, byte[] combineTag,byte[] combineData){
        this.esID = esId;
        this.combineTag = combineTag;
        this.combineData = combineData;
    }
}
