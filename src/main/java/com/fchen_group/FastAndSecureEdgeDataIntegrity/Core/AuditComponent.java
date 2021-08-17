package com.fchen_group.FastAndSecureEdgeDataIntegrity.Core;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

public abstract class AuditComponent {
    public abstract Key keyGen(int paramInt);

    public abstract void outSource();

    public abstract ChallengeData[] auditGen(int paramInt1, int paramInt2, Key paramKey) throws IOException;

    public abstract ProofData proGen(ChallengeData paramChallengeData, byte[][] paramArrayOfbyte, int paramInt) throws IOException;

    public abstract boolean Verify(ProofData[] paramArrayOfProofData, byte[][][] paramArrayOfbyte, ChallengeData[] paramArrayOfChallengeData, Key paramKey) throws IOException;

}
