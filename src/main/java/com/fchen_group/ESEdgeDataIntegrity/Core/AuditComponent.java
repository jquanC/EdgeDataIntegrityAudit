package com.fchen_group.ESEdgeDataIntegrity.Core;

import java.io.IOException;

public abstract class AuditComponent {
    public abstract Key keyGen(int len);

    public abstract void outSource(int esNum,Key key);

    public abstract ChallengeData[] auditGen(int esNum, int chaLen, Key key) throws IOException;

    public abstract ProofData proGen(ChallengeData oneEsChaData, byte[][] selectBlocks, int esID) throws IOException;

    public abstract boolean Verify(ProofData[] proofData, ChallengeData[] challengeSet,Key key) throws IOException;

}
