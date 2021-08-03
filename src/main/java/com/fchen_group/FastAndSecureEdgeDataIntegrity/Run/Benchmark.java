package com.fchen_group.FastAndSecureEdgeDataIntegrity.Run;

import com.fchen_group.FastAndSecureEdgeDataIntegrity.Core.ChallengeData;
import com.fchen_group.FastAndSecureEdgeDataIntegrity.Core.FSEDIAudit;
import com.fchen_group.FastAndSecureEdgeDataIntegrity.Core.Key;
import com.fchen_group.FastAndSecureEdgeDataIntegrity.Core.ProofData;

import javax.swing.filechooser.FileSystemView;
import java.io.*;
import java.util.Scanner;

public class Benchmark {
    private static String filePath = "E:\\Gitfolder\\TestData\\randomFile1G.rar";
    private static String replicaPath = "E:\\Gitfolder\\TestData\\randomFile1G.rar";
    private static float SAMPLED_RATE;
    private static int SECTOR_NUMBER;
    private static int BLOCK_NUMBER;
    private static int esNum;
    private static int EXP_TIME;


    public static void main(String[] args) throws IOException {

        Scanner in = new Scanner(System.in);
        System.out.println("Please enter the SAMPLED_RATE, range 0.01 to 1 ");
        SAMPLED_RATE = in.nextFloat();
        System.out.println("Please enter the SECTOR_NUMBER");
        SECTOR_NUMBER = in.nextInt();
        System.out.println("Please enter the num of  edge server ");
        esNum = in.nextInt();
        System.out.println("Please enter the num of  EXPERIMENT_TIME ");
        EXP_TIME = in.nextInt();
        in.close();

        //
        /*System.out.println("Please enter the filePath of  the original data ");
        filePath = in.nextLine();
        System.out.println("Please enter the filePath of  the replica data ");
        replicaPath = in.nextLine();*/
        long originalDataLenInBytes = (new File(filePath)).length();
        BLOCK_NUMBER = (int) Math.ceil(originalDataLenInBytes / 16);


        new Benchmark().run();


    }

    public void run() throws IOException {
        long[][] sTime = new long[EXP_TIME][5];
        long[][] time = new long[EXP_TIME][5];
        for (int t = 0; t < EXP_TIME; t++) {
            auditProcess(t, sTime, time);
        }
        //write out the experiment result
        File desktopDir = FileSystemView.getFileSystemView().getHomeDirectory();
        String desktopPath = desktopDir.getAbsolutePath();
        String newPath = desktopPath + "\\result" + SAMPLED_RATE + "-" + esNum + "-" + EXP_TIME + ".txt";
        System.out.println(newPath);

        FileWriter resWriter = new FileWriter(newPath);
        String titleLine = "PARAM:      BLOCK_NUM  " + BLOCK_NUMBER + "      SAMPLED_RATE  " + SAMPLED_RATE + "        " + "esNum " + esNum + "\r\n";
        resWriter.write(titleLine);

        for (int i = 0; i < EXP_TIME; i++) {
            StringBuffer sb = new StringBuffer();
            for (int j = 0; j < 5; j++) {
                sb.append(time[i][j]);
                if (j < 4) sb.append("                  ");
                else sb.append("\r\n");
            }
            String oneLine = sb.toString();
            resWriter.write(oneLine);

        }
        resWriter.flush();
        resWriter.close();

    }

    private void auditProcess(int ithTest, long[][] sTime, long[][] time) throws IOException {

        FSEDIAudit fsEDI = new FSEDIAudit(filePath, replicaPath, SECTOR_NUMBER, SAMPLED_RATE);

        //keyGen
        sTime[ithTest][0] = System.nanoTime();
        Key key = fsEDI.keyGen(SECTOR_NUMBER);
        time[ithTest][0] = System.nanoTime();
        time[ithTest][0] = time[ithTest][0] - sTime[ithTest][0];

        //outSource;
        sTime[ithTest][1] = System.nanoTime();
        fsEDI.outSource();
        time[ithTest][1] = System.nanoTime();
        time[ithTest][1] = time[ithTest][1] - sTime[ithTest][1];

        //generate ChallengeData
        sTime[ithTest][2] = System.nanoTime();
        ChallengeData[] chaDataArr = fsEDI.auditGen(esNum, Math.round(fsEDI.getBLOCK_NUMBER() * SAMPLED_RATE));
        time[ithTest][2] = System.nanoTime();
        time[ithTest][2] = time[ithTest][2] - sTime[ithTest][2];

        //ES receive challenge and then calculate proofData
        ProofData[] proofData = new ProofData[esNum];
        byte[][] selectedBlock;

        for (int i = 0; i < esNum; i++) {

            selectedBlock = fsEDI.readSelectedBlocks(filePath, chaDataArr[i]);
            long start = System.nanoTime();
            proofData[i] = fsEDI.proGen(chaDataArr[i], selectedBlock, i);
            long end = System.nanoTime();
            time[ithTest][3] = time[ithTest][3] + (end - start);
        }
        time[ithTest][3] = time[ithTest][3] / esNum;//平均每个esNum 执行proGen 的时间

        //Verify

        byte[][][] allSelectedBlock = fsEDI.readAllSelectedBlocks(filePath, chaDataArr);
        sTime[ithTest][4] = System.nanoTime();
        boolean isDataIntact = fsEDI.Verify(proofData, chaDataArr, allSelectedBlock);
        time[ithTest][4] = System.nanoTime();
        time[ithTest][4] = time[ithTest][4] - sTime[ithTest][4];

        if (isDataIntact) {
            System.out.println("In " + ithTest + "th audit ,the data is intact in all edge server");
        } else System.out.println("In " + ithTest + "th audit ,the data is corrupted in  edge server");

    }
}

