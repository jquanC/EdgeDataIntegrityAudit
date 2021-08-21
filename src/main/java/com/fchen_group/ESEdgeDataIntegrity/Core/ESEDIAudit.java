package com.fchen_group.ESEdgeDataIntegrity.Core;


import com.fchen_group.ESEdgeDataIntegrity.Support.Galois;
import com.fchen_group.ESEdgeDataIntegrity.Support.PseudoRandom;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Arrays;
import java.util.Random;

public class ESEDIAudit extends AuditComponent {
    private int BLOCK_NUMBER;
    private int SECTOR_NUMBER;
    private String filePath;
    private String replicaPath;
    private static int CHA_LEN;
    // private float SAMPLED_RATE;
    //private int L = 1 << 10; //param to control JVM heap limit
  /*  private byte[][] selectedOriginalData;
    private  byte[][] originalData;*/


    public ESEDIAudit(String filePath, String replicaPath, int SECTOR_NUMBER, int CHA_LEN) {
        this.filePath = filePath; // original data path
        this.replicaPath = replicaPath;
        this.SECTOR_NUMBER = SECTOR_NUMBER; // each sector is 1 Byte ,so this parameter decide the one block size as well as security level
        this.CHA_LEN = CHA_LEN;
        // calculate the block num
        long originalDataLenInBytes = (new File(filePath)).length();
        this.BLOCK_NUMBER = (int) Math.ceil(originalDataLenInBytes / 16);

    }

    public Key keyGen(int len) {
        System.out.println("start KetGen phase");
        String chars1 = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
        String chars2 = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        String chars3 = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        String chars4 = "xxxxxxxxxxxxxxxxxxxxxxxxxxabcdefghijklmnopqrstuvwxyz0123456789";

        StringBuffer strBuff1 = new StringBuffer();
        StringBuffer strBuff2 = new StringBuffer();
        StringBuffer strBuff3 = new StringBuffer();
        StringBuffer strBuff4 = new StringBuffer();
        for (int i = 0; i < len; i++) {
            strBuff1.append(chars1.charAt(new Random().nextInt(chars1.length())));
            strBuff2.append(chars2.charAt(new Random().nextInt(chars2.length())));
            strBuff3.append(chars3.charAt(new Random().nextInt(chars3.length())));
            strBuff4.append(chars4.charAt(new Random().nextInt(chars4.length())));
        }
        Key key = new Key(strBuff1.toString(), strBuff2.toString(), strBuff3.toString(), strBuff4.toString());

        System.out.println("KetGen phase finished");
        return key; //16
    }

    public void outSource(int esNum, Key key) {
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

      /*  //Generator matrix
        Random randomMatrix = new Random((key.getKeyMatrix()+ esNum).hashCode());
        byte[] MatrixA = new byte[SECTOR_NUMBER];
        randomMatrix.nextBytes(MatrixA);


        //Generator coefficient
        Random randomCoefficient = new Random((key.getCoe()+esNum).hashCode());
        byte[] coeMatrix = new byte[SECTOR_NUMBER];
        randomCoefficient.nextBytes(coeMatrix);
        //Generator fk
        Random random = new Random((key.getKeyPRF()+esNum).hashCode());*/
        Random random = new Random((key.getKeyPRF() + esNum).hashCode());
        int[][] indices = new int[esNum][chaLen];


        for (int i = 0; i < esNum; i++) {

            byte[] MatrixA = PseudoRandom.generateRandom(i, key.getKeyMatrix(), SECTOR_NUMBER);
            byte[] coeMatrix = PseudoRandom.generateRandom(i, key.getCoe(), SECTOR_NUMBER);
            byte[] fkMatrix = PseudoRandom.generateRandom(i, key.getFk(), SECTOR_NUMBER);
            //后面再改
            for (int j = 0; j < chaLen; j++) {
                indices[i][j] = random.nextInt(BLOCK_NUMBER);
            }
            challengeSet[i] = new ChallengeData(indices[i], MatrixA, coeMatrix, fkMatrix);
        }


        /*to generate a unique Matrix ; in scheme, the Matrix is (16*16)
         * but in simply , we can use a diagonal matrix , and use a (16*1) Matrix to store the diagonal elements */

        System.out.println("generate challenge data finished");

        return challengeSet;
    }

    /**
     * Each Edge Server use this method to return proofData
     *
     * @param oneEsChaData the challenge data send to the Edge Server, which have ： int[] blockIndex; byte[] matrixA;
     * @return ProofData include the ES id,and combined holding proof Tag
     */

    public ProofData proGen(ChallengeData oneEsChaData, byte[][] selectBlocks, int esID) throws IOException {


        //System.out.println("data prepared in " + esID + " th server");
        int chaBlockNum = oneEsChaData.blockIndex.length;
        //  int[] indices = oneEsChaData.blockIndex;
        byte[] matrixA = oneEsChaData.matrixA;
        byte[] fkMatrix = oneEsChaData.fkMatrix;
        byte[] coeMatrix = oneEsChaData.coefficient;

        byte[] proTag = new byte[SECTOR_NUMBER];
        byte[] temp = new byte[SECTOR_NUMBER];
        byte[] temp2 = new byte[SECTOR_NUMBER];

        byte[] combineTag = new byte[SECTOR_NUMBER];
        byte[] combineData = new byte[SECTOR_NUMBER];

        // first , generate selected tag array tagArr[], which  should be done before outsource actually;The SV outsource all tag blocks
        byte[][] tagArr = new byte[oneEsChaData.blockIndex.length][SECTOR_NUMBER];

        for (int i = 0; i < chaBlockNum; i++) {

            for (int j = 0; j < SECTOR_NUMBER; j++) {
                //temp[j] = Galois.multiply(matrixA[j], originalData[indices[i]][j]);//传入的是已经selected 的block
                temp[j] = Galois.multiply(matrixA[j], selectBlocks[i][j]);
            }
            for (int j = 0; j < SECTOR_NUMBER; j++) {
                proTag[j] = Galois.add(fkMatrix[j], temp[j]);
                tagArr[i][j] = proTag[j];
            }
        }

        // second,when receive challenge data,each ES server start to calculate combine tag  with coefficient
        // at the same time , calculate combineData
        for (int i = 0; i < chaBlockNum; i++) {
            for (int j = 0; j < SECTOR_NUMBER; j++) {
                temp[j] = Galois.multiply(tagArr[i][j], coeMatrix[j]);
                temp2[j] = Galois.multiply(selectBlocks[i][j], coeMatrix[j]);
            }
            for (int j = 0; j < SECTOR_NUMBER; j++) {
                combineTag[j] = Galois.add(temp[j], combineTag[j]);
                combineData[j] = Galois.add(temp2[j], combineData[j]);
            }
        }


        // System.out.println(esID + " th edge serve finished proGen");
        return new ProofData(esID, combineTag, combineData);
    }

    /**
     * @ original data
     */
    public boolean Verify(ProofData[] proofData, ChallengeData[] challengeSet, Key key) throws IOException {
        System.out.println("Verify phase start");
        int esNum = proofData.length;

        //combine all return POI
        byte[] resCombineReturnTag = new byte[SECTOR_NUMBER];
        for (int i = 0; i < esNum; i++) {

            byte[] oneESTag = proofData[i].combineTag;
            for (int j = 0; j < SECTOR_NUMBER; j++) {
                resCombineReturnTag[j] = Galois.add(resCombineReturnTag[j], oneESTag[j]);
            }
        }


        //calculate checking Result by original data
        byte[] checkBlock = new byte[SECTOR_NUMBER];

        for (int t = 0; t < esNum; t++) {

            ChallengeData oneChallenge = challengeSet[t];
            ProofData oneProofData = proofData[t];
            byte[] fkMatrix = oneChallenge.fkMatrix;
            byte[] matrixA = oneChallenge.matrixA;
            byte[] coeMatrix = oneChallenge.coefficient;

            byte[] tag = new byte[SECTOR_NUMBER];
            byte[] temp = new byte[SECTOR_NUMBER];
            byte[] temp2 = new byte[SECTOR_NUMBER];
            byte[] temp3 = new byte[SECTOR_NUMBER];

            // first, calculate e_i * fk as temp2
            for (int i = 0; i < CHA_LEN; i++) {
                for (int j = 0; j < SECTOR_NUMBER; j++) {
                    //temp[j] = Galois.multiply(matrixA[j], originalData[indices[i]][j]);//这是针对 original data的读取方法
                    temp[j] = Galois.multiply(coeMatrix[j], fkMatrix[j]); //已经是selected的
                }
                for (int j = 0; j < SECTOR_NUMBER; j++) {
                    temp2[j] = Galois.add(temp2[j], temp[j]);
                }
            }

            //cal A_i * proofData_i in temp[3] , at the same time add temp[2]
            byte[] oneCombineData = oneProofData.combineData;
            for (int j = 0; j < SECTOR_NUMBER; j++) {
                temp3[j] = Galois.multiply(matrixA[j], oneCombineData[j]);
                tag[j] = Galois.add(temp2[j], temp3[j]);

            }

            //and store in final res
            for(int j=0;j<SECTOR_NUMBER;j++){
                checkBlock[j] = Galois.add(tag[j], checkBlock[j]);
            }
        }
        System.out.println("Verify phase finished");
        //Verify
        return Arrays.equals(resCombineReturnTag, checkBlock);
    }

    public int getBLOCK_NUMBER() {
        return BLOCK_NUMBER;
    }

    public byte[][] readSelectedBlocks(String filePath, ChallengeData oneCha) throws IOException {
        byte[][] selectedBlock = new byte[oneCha.blockIndex.length][SECTOR_NUMBER];
        RandomAccessFile randomIn = new RandomAccessFile(filePath, "r");
        for (int i = 0; i < oneCha.blockIndex.length; i++) {
            randomIn.seek(oneCha.blockIndex[i] * (long) SECTOR_NUMBER); //pos is long type,but oneCha.blockIndex[i] and SECTOR_NUMBER is int ,when the product> 1<<31 is wrong

            randomIn.read(selectedBlock[i]);
        }
        randomIn.close();

        return selectedBlock;
    }

    public byte[][][] readAllSelectedBlocks(String filePath, ChallengeData[] allCha) throws IOException {
        RandomAccessFile randomIn = new RandomAccessFile(filePath, "r");
        byte[][][] allSelectedBlock = new byte[allCha.length][allCha[0].blockIndex.length][SECTOR_NUMBER];
        for (int t = 0; t < allCha.length; t++) {
            ChallengeData oneCha = allCha[t];
            for (int i = 0; i < oneCha.blockIndex.length; i++) {
                randomIn.seek(oneCha.blockIndex[i] * (long) SECTOR_NUMBER);
                randomIn.read(allSelectedBlock[t][i]);
            }

        }

        randomIn.close();
        return allSelectedBlock;
    }

    public byte[][] readAllSourceBlocks(String filePath) throws IOException {
        FileInputStream in = new FileInputStream(new File(filePath));
        byte[][] originalData = new byte[BLOCK_NUMBER][SECTOR_NUMBER];
        for (int i = 0; i < BLOCK_NUMBER; i++) {
            in.read(originalData[i]);
        }
        in.close();
        return originalData;
    }

}
