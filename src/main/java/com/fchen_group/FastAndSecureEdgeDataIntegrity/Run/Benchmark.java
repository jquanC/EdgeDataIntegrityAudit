package com.fchen_group.FastAndSecureEdgeDataIntegrity.Run;

import com.fchen_group.FastAndSecureEdgeDataIntegrity.Core.ChallengeData;
import com.fchen_group.FastAndSecureEdgeDataIntegrity.Core.FSEDIAudit;
import com.fchen_group.FastAndSecureEdgeDataIntegrity.Core.Key;
import com.fchen_group.FastAndSecureEdgeDataIntegrity.Core.ProofData;

import javax.swing.filechooser.FileSystemView;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;
import java.util.Scanner;

public class Benchmark {
    private static String filePath;//"E:\\Gitfolder\\TestData\\randomFile1G.rar"
    private static String replicaPath;
    //private static float SAMPLED_RATE;//we don't need it in this version
    private static int CHA_LEN = 1 << 9;//512 can ensure >99% to detect corrupted block (even if just 1 block corrupted) when FileSize reach >40G
    private static int SECTOR_NUMBER;
    private static int BLOCK_NUMBER;
    private static int esNum;
    private static int EXP_TIME;
    //private static int L = 1 << 10; //we don't need it in this version


    public static void main(String[] args) throws IOException {

        Scanner in = new Scanner(System.in);
        /*System.out.println("Please enter the SAMPLED_RATE, range 0.01 to 1 ");
        SAMPLED_RATE = in.nextFloat();*/
        System.out.println("Please enter the SECTOR_NUMBER");
        SECTOR_NUMBER = in.nextInt();
        System.out.println("Please enter the num of  edge server ");
        esNum = in.nextInt();
        System.out.println("Please enter the num of  EXPERIMENT_TIME ");
        EXP_TIME = in.nextInt();
        //in.close();


        System.out.println("Please enter the filePath of  the original data ");
        in.nextLine();//读string 方法会把换行符当作字符串读进来，需要过滤掉
        filePath = in.nextLine();
        System.out.println("Please enter the filePath of  the replica data ");

        replicaPath = in.nextLine();
        in.close();

        long originalDataLenInBytes = (new File(filePath)).length();
        BLOCK_NUMBER = (int) Math.ceil(originalDataLenInBytes / 16);


        new Benchmark().run();


    }

    public void run() throws IOException {
        long[][] sTime = new long[EXP_TIME][5];
        long[][] time = new long[EXP_TIME][5];
        SimpleDateFormat dfStart = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String startProgramTime = dfStart.format(new Date());

        for (int t = 0; t < EXP_TIME; t++) {
            auditProcess(t, sTime, time);
        }
        //write out the experiment result
        File desktopDir = FileSystemView.getFileSystemView().getHomeDirectory();
        String desktopPath = desktopDir.getAbsolutePath();
        //String newPath = desktopPath + "\\result" + SAMPLED_RATE + "-" + esNum + "-" + EXP_TIME + ".txt";
        String newPath = desktopPath + "\\result" + CHA_LEN + "-" + esNum + "-" + EXP_TIME + ".txt";
        System.out.println(newPath);

        FileWriter resWriter = new FileWriter(newPath);
        File sourceFile = new File(filePath);
        String fileName = sourceFile.getName();
        String titleLine = "PARAM:      BLOCK_NUM  " + BLOCK_NUMBER + "      CHA_LEN  " + CHA_LEN + "        " + "esNum " + esNum + "     fileName     " + fileName + "\r\n";
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

        String endProgramTime = dfStart.format(new Date());
        resWriter.write("Program started at " + startProgramTime + " , finished at " + endProgramTime);

        resWriter.flush();
        resWriter.close();

    }

    private void auditProcess(int ithTest, long[][] sTime, long[][] time) throws IOException {

        FSEDIAudit fsEDI = new FSEDIAudit(filePath, replicaPath, SECTOR_NUMBER, CHA_LEN);

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
        //ChallengeData[] chaDataArr = fsEDI.auditGen(esNum, Math.round(fsEDI.getBLOCK_NUMBER() * SAMPLED_RATE),key);
        ChallengeData[] challengeSet = fsEDI.auditGen(esNum, CHA_LEN, key);
        time[ithTest][2] = System.nanoTime();
        time[ithTest][2] = time[ithTest][2] - sTime[ithTest][2];


        //ProGen : ES receive challenge and then calculate proofData
        ProofData[] proofData = new ProofData[esNum];
        //byte[][] replicaDataBlocks = fsEDI.readAllSourceBlocks(replicaPath);


        for (int i = 0; i < esNum; i++) {
            byte [][] replicaSelectBlocks = fsEDI.readSelectedBlocks(replicaPath,challengeSet[i]);
            long start = System.nanoTime();
            proofData[i] = fsEDI.proGen(challengeSet[i], replicaSelectBlocks, i);
            long end = System.nanoTime();
            time[ithTest][3] = time[ithTest][3] + (end - start);
        }
        time[ithTest][3] = time[ithTest][3] / esNum;//平均每个esNum 执行proGen 的时间


        //Verify
        byte[][][] allSelectBlock = fsEDI.readAllSelectedBlocks(filePath, challengeSet);
        //byte[][] originalDataBlocks = fsEDI.readAllSourceBlocks(filePath);

        sTime[ithTest][4] = System.nanoTime();

        boolean isDataIntact = fsEDI.Verify(proofData, allSelectBlock, challengeSet, key);

        time[ithTest][4] = System.nanoTime();
        time[ithTest][4] = time[ithTest][4] - sTime[ithTest][4];

        if (isDataIntact) {
            System.out.println("In " + ithTest + "th audit ,the data is intact in all edge server");
        } else System.out.println("In " + ithTest + "th audit ,the data is corrupted in  edge server");

    }
}

