package com.fchen_group.ESEdgeDataIntegrity.Support;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

public class PseudoRandom {

	/**
	 * AES encryption
	 * @param strKey
	 * @param content
	 * @return
	 */
	public static byte[] encrypt(String strKey,int content) {
		try {
			
			
			byte[] enCodeFormat=strKey.getBytes();
			SecretKeySpec key=new SecretKeySpec(enCodeFormat, "AES");
			//Password creator
			Cipher cipher=Cipher.getInstance("AES");
			//initialize
			cipher.init(Cipher.ENCRYPT_MODE, key);
			byte[] result=cipher.doFinal(String.valueOf(content).getBytes());
			return result;
			
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchPaddingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvalidKeyException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalBlockSizeException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (BadPaddingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
		
	}

	/**
	 * AES decode
	 * @param strKey
	 * @param content
	 * @return
	 */
	public static byte[] decrypt(String strKey,byte[] content) {
		try {
			
			byte[] enCodeFormat=strKey.getBytes();
			SecretKeySpec key=new SecretKeySpec(enCodeFormat, "AES");
			//Password creator
			Cipher cipher=Cipher.getInstance("AES");
			//initialize
			cipher.init(Cipher.DECRYPT_MODE, key);
			//decode
			byte[] result=cipher.doFinal(content);
			return result;
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchPaddingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvalidKeyException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalBlockSizeException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (BadPaddingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return null;
	}

	public static int BytetoInt(byte[] bytes) {
		return(bytes[0]&0xff)<<24|(bytes[1]&0xff)<<16|(bytes[2]&0xff)<<8|(bytes[3]&0xff);
	}
	
	/**
	 * //For testing only
	public static void main(String[] args) {

		String key="KeyPRF";
		System.out.println((int)Math.ceil(1/(double)16));
		byte[] result=encrypt(key, 2);
		System.out.println(result.length);
		
	}*/

	public static byte[] generateRandom(int index,String Key,int paritySize) {
		int AEScount=(int) Math.ceil(paritySize/(double)(128/8));
		//Encrypted indexes are block indexes multiplied by offsets to ensure that each random number is different
		index=AEScount*index;
		//After AES is encrypted, it returns a fixed length of 16 bytes
		byte[] result=new byte[AEScount*16];
		for(int i=0;i<AEScount;i++){
			byte[] temp=encrypt(Key, index);
			index++;
			System.arraycopy(temp, 0, result, i*temp.length, temp.length);
		}
//		for(int j=0;j<result.length;j++){
//			System.out.print(String.format("%02x ", result[j]));
//		}
//		System.out.println("");
		return result;
	}
	
}



