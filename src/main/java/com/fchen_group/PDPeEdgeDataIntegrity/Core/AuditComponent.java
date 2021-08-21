package com.fchen_group.PDPeEdgeDataIntegrity.Core;

import java.io.IOException;
import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;

public abstract class AuditComponent {
    public abstract Key keyGen(int secureLevel) throws NoSuchAlgorithmException;
    public abstract void outSource();
    public abstract ChallengeData[] auditGen(int esNum,int chaLen,Key key) throws IOException;
    public abstract ProofData proGen(ChallengeData oneEsChaData,BigInteger[] selectBlocks,int edID, BigInteger g, BigInteger N) throws IOException;
    public abstract boolean Verify(ProofData[] proofData, BigInteger[][] allSelectBlocks, ChallengeData[] challengeSet, Key key)throws IOException ;

}
