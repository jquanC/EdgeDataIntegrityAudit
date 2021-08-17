package com.fchen_group.ICLEdgeDataIntegrity.Core;


import com.fchen_group.ICLEdgeDataIntegrity.Support.Galois;

import java.beans.BeanInfo;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Arrays;
import java.util.Random;

public class ICLEDIAudit extends AuditComponent {
    private int BLOCK_NUMBER;
    // private int SECTOR_NUMBER;
    private String filePath;
    private String replicaPath;
    private static int CHA_LEN;
    private int BLOCK_SIZE;

    private BigInteger g;
    private BigInteger N;


    public ICLEDIAudit(String filePath, String replicaPath, int BLOCK_SIZE, int CHA_LEN) {
        this.filePath = filePath; // original data path
        this.replicaPath = replicaPath;
        this.CHA_LEN = CHA_LEN;
        this.BLOCK_SIZE = BLOCK_SIZE;
        // calculate the block num
        long originalDataLenInBytes = (new File(filePath)).length();
        this.BLOCK_NUMBER = (int) Math.ceil(originalDataLenInBytes / BLOCK_SIZE);
        System.out.println("BLOCK_NUMBER=" + BLOCK_NUMBER);

    }

    public Key keyGen(int secureLevel) throws NoSuchAlgorithmException {
        int len = secureLevel / 8; // we use 128 bit secure level
        System.out.println("start KetGen phase");
        String chars1 = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";//prf seed

        StringBuffer strBuff1 = new StringBuffer();
        for (int i = 0; i < len; i++) {
            strBuff1.append(chars1.charAt(new Random().nextInt(chars1.length())));
        }

        RSAKeyPairGenerator rsaKeyPair = new RSAKeyPairGenerator();
        RSAPublicKey publicKey = rsaKeyPair.getPublicKey();
        RSAPrivateKey privateKey = rsaKeyPair.getPrivateKey();


        BigInteger N = publicKey.getModulus();
        BigInteger delta = findDelta(N);
        BigInteger g = delta.multiply(delta);


        Key key = new Key(strBuff1.toString(), g, N);
        this.g = g;
        this.N = N;

        System.out.println("KetGen phase finished");
        return key;

    }

    public void outSource() {
        System.out.println("start outSource phase");

        System.out.println("end outSource phase");

    }

    /**
     * @param chaLen chaLen = BLOCK_NUMBER*SAMPLED_RATE;
     * @reruen return the challenge data set of all esNum
     * 修改为 return 一个文件路径，存储ChallengeData的;
     * 在实际网络中，发送给某个ES时，只需从文件中读
     */
    public ChallengeData[] auditGen(int esNum, int chaLen, Key key) throws IOException {
        System.out.println("start audit phase");
        /* to generate indices for all ES*/
        ChallengeData[] challengeSet = new ChallengeData[esNum];


        Random random = new Random(key.getKeyPRF().hashCode());

        int[][] indices = new int[esNum][chaLen];

        for (int i = 0; i < esNum; i++) {
            for (int j = 0; j < chaLen; j++) {
                indices[i][j] = random.nextInt(BLOCK_NUMBER);
            }
            challengeSet[i] = new ChallengeData(indices[i]);
        }


        System.out.println("generate challenge data finished");

        return challengeSet;
    }

    /**
     * Each Edge Server use this method to return proofData
     *
     * @param oneEsChaData the challenge data send to the Edge Server, which have ： int[] blockIndex; byte[] matrixA;
     * @return ProofData include the ES id,and combined holding proof Tag
     */

    public ProofData proGen(ChallengeData oneEsChaData, BigInteger[] selectBigNumBlocks, int esID, BigInteger g, BigInteger N) throws IOException {


        //System.out.println("data prepared in " + esID + " th server");
        int chaBlockNum = oneEsChaData.blockIndex.length;


        BigInteger proTag = BigInteger.ZERO;
        for (int i = 0; i < chaBlockNum; i++) {
            //proTag = (proTag.add(selectBlocks[i])).mod(N);
            proTag = proTag.add(selectBigNumBlocks[i]);

        }
        proTag = g.modPow(proTag, N);
        // System.out.println(esID + " th edge serve finished proGen");
        return new ProofData(esID, proTag);
    }

    /**
     * @ original data
     */
    public boolean Verify(ProofData[] proofData, BigInteger[][] allSelectBigNumBlocks, ChallengeData[] challengeSet, Key key) throws IOException {
        System.out.println("Verify phase start");
        int esNum = proofData.length;
        BigInteger N = key.getN();
        BigInteger g = key.getG();

        //combine all return POI
        BigInteger resCombineReturnTag = BigInteger.ONE;
        for (int i = 0; i < esNum; i++) {
            BigInteger oneESProofTag = proofData[i].combineTag;
            //resCombineReturnTag = (resCombineReturnTag.multiply(oneESProofTag)).mod(N);
            resCombineReturnTag = resCombineReturnTag.multiply(oneESProofTag);//初始化0乘肯定是0啊

        }
        resCombineReturnTag = resCombineReturnTag.mod(N);

        //calculate checking Result by original data
        BigInteger checkBlock = BigInteger.ZERO;

        for (int t = 0; t < esNum; t++) {

            // indices = challengeSet[t].blockIndex;
            BigInteger tag = BigInteger.ZERO;
            for (int i = 0; i < CHA_LEN; i++) {
                //tag = (tag.add(allSelectBigNumBlocks[t][i])).mod(N);
                tag = tag.add(allSelectBigNumBlocks[t][i]);
            }

            //checkBlock = (checkBlock.add(tag)).mod(N);
            checkBlock = checkBlock.add(tag);
        }

        checkBlock = g.modPow(checkBlock, N);
        System.out.println("Verify phase finished");
        //Verify
        return resCombineReturnTag.equals(checkBlock);
    }

    public int getBLOCK_NUMBER() {
        return BLOCK_NUMBER;
    }

    public BigInteger[] readSelectedBlocks(String filePath, ChallengeData oneCha) throws IOException {
        byte[][] selectedBlock = new byte[oneCha.blockIndex.length][BLOCK_SIZE];
        RandomAccessFile randomIn = new RandomAccessFile(filePath, "r");
        for (int i = 0; i < oneCha.blockIndex.length; i++) {
            randomIn.seek(oneCha.blockIndex[i] * (long) BLOCK_SIZE); //pos is long type,but oneCha.blockIndex[i] and SECTOR_NUMBER is int ,when the product> 1<<31 is wrong

            randomIn.read(selectedBlock[i]);
        }
        randomIn.close();
        BigInteger[] selectedBigNums = new BigInteger[oneCha.blockIndex.length];
        for (int i = 0; i < oneCha.blockIndex.length; i++) {
            selectedBigNums[i] = new BigInteger(selectedBlock[i]);
        }

        return selectedBigNums;
    }

    public BigInteger[][] readAllSelectedBlocks(String filePath, ChallengeData[] allCha) throws IOException {
        RandomAccessFile randomIn = new RandomAccessFile(filePath, "r");
        byte[][][] allSelectedBlock = new byte[allCha.length][allCha[0].blockIndex.length][BLOCK_SIZE];
        BigInteger[][] allSelectedBigNumsBlocks = new BigInteger[allCha.length][allCha[0].blockIndex.length];
        for (int t = 0; t < allCha.length; t++) {
            ChallengeData oneCha = allCha[t];
            for (int i = 0; i < allCha[0].blockIndex.length; i++) {
                randomIn.seek(oneCha.blockIndex[i] * (long) BLOCK_SIZE);
                randomIn.read(allSelectedBlock[t][i]);
                allSelectedBigNumsBlocks[t][i] = new BigInteger(allSelectedBlock[t][i]);
            }


        }
        randomIn.close();
        return allSelectedBigNumsBlocks;
    }

    public BigInteger[] readAllSourceBlocks(String filePath) throws IOException {
        FileInputStream in = new FileInputStream(new File(filePath));
        BigInteger[] allSourceBigNumBlocks = new BigInteger[BLOCK_NUMBER];
        byte[][] originalData = new byte[BLOCK_NUMBER][BLOCK_SIZE];
        for (int i = 0; i < BLOCK_NUMBER; i++) {
            in.read(originalData[i]);
            allSourceBigNumBlocks[i] = new BigInteger(originalData[i]);
        }
        in.close();
        return allSourceBigNumBlocks;

    }

    public BigInteger getG() {
        return g;
    }

    public BigInteger getN() {
        return N;
    }

    public BigInteger findDelta(BigInteger N) {

        BigInteger numSeeking = new BigInteger(3072, new Random(2222));

        int count = 0;
        boolean flag1 = true;
        boolean flag2 = true;
        while ((count < 1 << 20) && flag1 && flag2) {
            numSeeking = numSeeking.nextProbablePrime();
            count++;
            if (N.gcd(numSeeking).equals(BigInteger.ONE)) {
                if (N.gcd(numSeeking.subtract(BigInteger.ONE.add(BigInteger.ONE))).equals(BigInteger.ONE)) {
                    flag1 = false;
                } else if (N.gcd(numSeeking.add(BigInteger.ONE.add(BigInteger.ONE))).equals(BigInteger.ONE)) {
                    flag2 = false;
                }
            }
        }
        BigInteger delta = BigInteger.ONE;

        if (flag1 == false) {
            //System.out.println("from sub");
            delta = numSeeking.subtract(BigInteger.ONE);
        }
        if (flag2 == false) {
           // System.out.println("from add");
            delta = numSeeking.add(BigInteger.ONE);
        }

        return delta;
    }

}
