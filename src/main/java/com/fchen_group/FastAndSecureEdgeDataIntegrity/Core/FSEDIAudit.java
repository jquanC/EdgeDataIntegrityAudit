package com.fchen_group.FastAndSecureEdgeDataIntegrity.Core;


import com.fchen_group.FastAndSecureEdgeDataIntegrity.Support.Galois;

import java.io.*;
import java.util.Arrays;
import java.util.Random;

public class FSEDIAudit extends AuditComponent {
    private int BLOCK_NUMBER;
    private int SECTOR_NUMBER;
    private String filePath;
    private String replicaPath;
    private float SAMPLED_RATE;
  /*  private byte[][] selectedOriginalData;
    private  byte[][] originalData;*/


    public FSEDIAudit(String filePath, String replicaPath ,int SECTOR_NUMBER, float SAMPLED_RATE) {
        this.filePath = filePath; // original data path
        this.replicaPath = replicaPath;
        this.SECTOR_NUMBER = SECTOR_NUMBER; // each sector is 1 Byte ,so this parameter decide the one block size as well as security level
        this.SAMPLED_RATE = SAMPLED_RATE;
        // calculate the block num
        long originalDataLenInBytes = (new File(filePath)).length();
        this.BLOCK_NUMBER = (int) Math.ceil(originalDataLenInBytes / 16);

    }

    public Key keyGen(int len) {
        System.out.println("start KetGen phase");
        String chars = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
        StringBuffer strBuff = new StringBuffer();
        for (int i = 0; i < SECTOR_NUMBER; i++) {
            strBuff.append(chars.charAt(new Random().nextInt(chars.length())));
        }
        Key key = new Key(strBuff.toString());

        System.out.println("KetGen phase finished");
        return key;
    }

    public void outSource() {
        System.out.println("start outSource phase");

        System.out.println("end outSource phase");

    }

    /**
     * @param chaLen chaLen = BLOCK_NUMBER*SAMPLED_RATE;
     */
    public ChallengeData[] auditGen(int esNum, int chaLen) {
        System.out.println("start audit phase");
        /* to generate indices for all ES*/
        Random random = new Random();
        ChallengeData[] challengeSet = new ChallengeData[esNum];
        int[][] indices = new int[esNum][chaLen];
        for (int i = 0; i < esNum; i++) {
            for (int j = 0; j < chaLen; j++) {
                indices[i][j] = random.nextInt(BLOCK_NUMBER);
            }
        }

        /*to generate a unique Matrix ; in scheme, the Matrix is (16*16)
         * but in simply , we can use a diagonal matrix , and use a (16*1) Matrix to store the diagonal elements */
        byte[] MatrixA = new byte[chaLen];

        /* an easy implementation,generate random bytes to fill the array*/
        random.nextBytes(MatrixA);

        for (int i = 0; i < esNum; i++) {
            challengeSet[i] = new ChallengeData(indices[i], MatrixA);
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

    public ProofData proGen(ChallengeData oneEsChaData,byte[][] selectedData, int esID) throws IOException {

       /* System.out.println(esID + " th edge serve start proGen");
        long startTime, endTime, time;
        startTime = System.nanoTime();
        byte[][] replica = new byte[BLOCK_NUMBER][SECTOR_NUMBER];
        FileInputStream in = new FileInputStream(new File(filePath));
        for (int i = 0; i < BLOCK_NUMBER; i++) {
            in.read(replica[i]);
        }
        in.close();
        endTime = System.nanoTime();
        time = endTime - startTime;
        System.out.println("read all data into Memory need " + time + "ns");*/

        System.out.println("data prepared in " + esID + " th server");
        int chaBlockNum = oneEsChaData.blockIndex.length;
        int[] indices = oneEsChaData.blockIndex;
        byte[] matrixA = oneEsChaData.matrixA;

        byte[] proTag = new byte[SECTOR_NUMBER];
        byte[] temp = new byte[SECTOR_NUMBER];
        for (int i = 0; i < chaBlockNum; i++) {
            //achieve one time block multiply
            for (int j = 0; j < SECTOR_NUMBER; j++) {
                //temp[j] = Galois.multiply(matrixA[j],selectedData[indices[i]][j]);//传入的是已经selected 的block
                temp[j] = Galois.multiply(matrixA[j],selectedData[i][j]);
            }
            for (int j = 0; j < SECTOR_NUMBER; j++) {
                proTag[j] = Galois.add(proTag[j], temp[j]);
            }

        }
        System.out.println(esID + " th edge serve finished proGen");
        return new ProofData(esID, proTag);
    }


    public boolean Verify(ProofData[] proofData, ChallengeData[] allChaData, byte[][][] allSelectedBlock) throws IOException {
        System.out.println("Verify phase start");
        int esNum = proofData.length;
        int chaBlockNum = allChaData[0].blockIndex.length;

        /*//read original data
        byte[][] originalData = new byte[BLOCK_NUMBER][SECTOR_NUMBER];
        FileInputStream in = new FileInputStream(new File(filePath));
        for (int i = 0; i < BLOCK_NUMBER; i++) {
            in.read(originalData[i]);
        }
        in.close();*/

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
            //i th Challenge data
            int[] indices = allChaData[t].blockIndex;
            byte[] matrixA = allChaData[t].matrixA;

            byte[] tag = new byte[SECTOR_NUMBER];
            byte[] temp = new byte[SECTOR_NUMBER];
            for (int i = 0; i < chaBlockNum; i++) {

                for (int j = 0; j < SECTOR_NUMBER; j++) {
                    //temp[j] = Galois.multiply(matrixA[j], allSelectedBlock[t][indices[i]][j]);//这是针对 original data的读取方法
                    temp[j] = Galois.multiply(matrixA[j], allSelectedBlock[t][i][j]); //已经是selected的
                }
                for (int j = 0; j < SECTOR_NUMBER; j++) {
                    tag[j] = Galois.add(tag[j], temp[j]);
                }

            }

            for (int j = 0; j < SECTOR_NUMBER; j++) {
                checkBlock[j] = Galois.add(checkBlock[j], tag[j]);
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
            randomIn.seek(oneCha.blockIndex[i] * SECTOR_NUMBER);

            randomIn.read(selectedBlock[i]);
        }
        randomIn.close();

    return selectedBlock;
    }

    public byte[][][] readAllSelectedBlocks(String filePath,ChallengeData[] allCha) throws IOException {
        RandomAccessFile randomIn = new RandomAccessFile(filePath, "r");
        byte[][][] allSelectedBlock = new byte[allCha.length][allCha[0].blockIndex.length][SECTOR_NUMBER];
        for(int t=0;t<allCha.length;t++){
            ChallengeData oneCha = allCha[t];
            for (int i = 0; i < oneCha.blockIndex.length; i++) {
                randomIn.seek(oneCha.blockIndex[i] * SECTOR_NUMBER);
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
