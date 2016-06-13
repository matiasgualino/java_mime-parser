package ar.edu.itba.pdc.transformer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

import ar.edu.itba.pdc.mail.Mail;

public class LeetTransformer implements Transformer {

	private static LeetTransformer instance;
	
	private File transformFile;
	private File mailFile;
	
	private RandomAccessFile mailReader;
	private RandomAccessFile transformWriter;
	
	private LeetTransformer() {}
	
	public static Transformer getInstance() {
		if(instance == null) {
			instance = new LeetTransformer();
		}
		return instance;
	}
	
	@Override
	public void transform(Mail mail) throws FileNotFoundException, IOException {
		transformFile = new File(Mail.DIRECTORY_NAME + "/" + mail.getNumber() + "_transform.txt");
		transformFile.createNewFile();
		mailFile = new File(Mail.DIRECTORY_NAME + "/" + mail.getNumber() + ".txt");
		
		mailReader = new RandomAccessFile(mailFile, "r");
		transformWriter = new RandomAccessFile(transformFile, "rw");
		
		String line;
		int i = 1;
		transformWriter.write((mailReader.readLine() + Mail.CR_LF).getBytes());
		
		int subjectStart = mail.getSubjectStartIndex();
		int subjectEnd = mail.getSubjectEndIndex();
		
		while ((line = mailReader.readLine()) != null) {
			if (i == subjectStart) {
				while (i <= subjectEnd) {
					if (line == null) {
						finish();
						return;
					}
					transformWriter.write((leet(line) + Mail.CR_LF).getBytes());
					i++;
					line = mailReader.readLine();
				}
			}
			i++;
			if (line != null) {
				transformWriter.write((line + Mail.CR_LF).getBytes());
			}
		}
		finish();
	}
	
	private String leet(String line) {
		char[] c = line.toCharArray();
		for (int i = 0; i < c.length; i++) {
			switch (c[i]) {
				case 'a':
					c[i] = '4';
					break;
				case 'e':
					c[i] = '3';
					break;
				case 'i':
					c[i] = '1';
					break;
				case 'o':
					c[i] = '0';
					break;
				case 'c':
					c[i] = '<';
					break;
			}
		}
		return new String(c);
	}
	
	private void finish() throws IOException {
		mailFile.delete();
		transformFile.renameTo(mailFile);
		mailReader.close();
		transformWriter.close();
	}

}
