package com.fchen_group.FastAndSecureEdgeDataIntegrity.Core;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

public abstract class AuditComponent {
    public abstract Key keyGen(int len);
    public abstract void outSource();
    public abstract String auditGen(int esNum,int chaLen,Key key) throws IOException;
    public abstract ProofData proGen(ChallengeData oneEsChaData,byte[][] selectedData,int edID) throws IOException;
    public abstract boolean Verify(ProofData[] proofData,byte[][] originalData,String chaFilePath,Key key) throws IOException;

}
