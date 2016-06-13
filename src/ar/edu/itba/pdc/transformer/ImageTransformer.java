package ar.edu.itba.pdc.transformer;

import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

import java.util.concurrent.atomic.AtomicBoolean;

import javax.imageio.ImageIO;

import ar.edu.itba.pdc.mail.Mail;
import ar.edu.itba.pdc.mail.MailImage;

import org.apache.commons.codec.binary.Base64;

public class ImageTransformer implements Transformer {

	public static AtomicBoolean enabled = new AtomicBoolean(false);
	
	private static ImageTransformer instance;
	
	private File transformFile;
	private File mailFile;
	
	private RandomAccessFile mailReader;
	private RandomAccessFile transformWriter;
	
	private ImageTransformer() {}
	
	public static Transformer getInstance() {
		if(instance == null) {
			instance = new ImageTransformer();
		}
		return instance;
	}
	
	@Override
	public void transform(Mail mail) throws FileNotFoundException, IOException {
		if(mail.getMailImages().isEmpty()) {
			return;
		}
		
		transformFile = new File(Mail.DIRECTORY_NAME + "/" + mail.getNumber() + "_transform.txt");
		transformFile.createNewFile();
		mailFile = new File(Mail.DIRECTORY_NAME + "/" + mail.getNumber() + ".txt");
		
		mailReader = new RandomAccessFile(mailFile, "r");
		transformWriter = new RandomAccessFile(transformFile, "rw");
		
		String line = "";
		int i = 0;
		transformWriter.write((mailReader.readLine() + Mail.CR_LF).getBytes());
		
		for(MailImage image : mail.getMailImages()) {
			int start = image.getStartLine();
			int end = image.getEndLine();
			while(i < start && (line = mailReader.readLine()) != null) {
				transformWriter.write((line + Mail.CR_LF).getBytes());
				i++;
			}
			String base64 = line;
			while(i < end && (line = mailReader.readLine()) != null) {
				base64 += line;
				i++;
			}
			
			String rotated = rotate(base64);
			transformWriter.write((rotated + Mail.CR_LF).getBytes());
			transformWriter.write((line + Mail.CR_LF).getBytes());
		}
		
		while((line = mailReader.readLine()) != null) {
			transformWriter.write((line + Mail.CR_LF).getBytes());
		}
		
		mailFile.delete();
		transformFile.renameTo(mailFile);
		mailReader.close();
		transformWriter.close();
	}
	
	public byte[] decodeBase64(String s) {
		return Base64.decodeBase64(s);
	}
	public String encodeBase64(byte[] b) {
		return Base64.encodeBase64String(b);
	}

	private String rotate(String image) {
		byte[] b  = decodeBase64(image);

		ByteArrayInputStream in = new ByteArrayInputStream(b);

		BufferedImage img = null;
		try {
			img = ImageIO.read(in);
		} catch (Exception e) {
			e.printStackTrace();
		}
		BufferedImage outputImg = rotateImage(img, 180);

		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		try {
			ImageIO.write(outputImg, "jpg", bos);
		} catch (IOException e) {
			e.printStackTrace();
			return image;
		}
		try {
			bos.flush();
		} catch (IOException e) {
			e.printStackTrace();
			return image;
		}
		try {
			bos.close();
		} catch (IOException e) {
			e.printStackTrace();
			return image;
		}

		byte[] o = bos.toByteArray();
		return encodeBase64(o);
	}
	
	public static BufferedImage rotateImage(BufferedImage image, double angle) {
		AffineTransform tx = new AffineTransform();

		tx.translate(image.getWidth()/2, image.getHeight()/2);
		tx.rotate(Math.PI);
		tx.translate(-image.getWidth()/2,-image.getHeight()/2);

		AffineTransformOp op = new AffineTransformOp(tx, AffineTransformOp.TYPE_BILINEAR);
		BufferedImage outputImage = new BufferedImage(image.getWidth(), image.getHeight(), image.getType());
		
		return op.filter(image, outputImage);
	}
}
