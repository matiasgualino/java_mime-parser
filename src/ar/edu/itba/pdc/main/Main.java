package ar.edu.itba.pdc.main;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import ar.edu.itba.pdc.mail.Mail;
import ar.edu.itba.pdc.mail.MailImage;
import ar.edu.itba.pdc.transformer.ImageTransformer;
import ar.edu.itba.pdc.transformer.LeetTransformer;


public class Main {

	public static void main(String[] args) throws IOException, InterruptedException {
	
		ClassLoader classLoader = Main.class.getClassLoader();
		//File mailOnlyText = new File(classLoader.getResource("mail_only_text.txt").getFile());
		File mailWithImage = new File(classLoader.getResource("mail_with_image.txt").getFile());

		//BufferedReader reader = new BufferedReader(new FileReader(mailOnlyText));
		BufferedReader reader = new BufferedReader(new FileReader(mailWithImage));
		
		// Creo el mail
		Mail mail = new Mail();
		
		String line;

		// Recorro todas las lineas del archivo mail
		while((line = reader.readLine()) != null) {
			// Las agrego al mail
			mail.addLine(line + "\r\n");
		}
		
		// Parseo el mail
		mail.parse();
		
		reader.close();
		
		// Transformo Leet
		LeetTransformer.getInstance().transform(mail);
		// Roto imagen
		ImageTransformer.getInstance().transform(mail);
		
		// Muestro resultados
		System.out.println("FROM: " + mail.getFrom());
		System.out.println("DATE: " + mail.getDate());
		System.out.println("SUBJECT: " + mail.getSubject());
		System.out.println("TO: " + mail.getTo());
		System.out.println("SUBJECT START: " + mail.getSubjectStartIndex());
		System.out.println("SUBJECT END: " + mail.getSubjectEndIndex());
		System.out.println("SIZE: " + mail.getCurrentSize());
		System.out.println("HEADERS: " + mail.getHeader());
		System.out.println("IMAGES COUNT: " + mail.getMailImages().size());
		for(MailImage image : mail.getMailImages()) {
			int start = image.getStartLine();
			int end = image.getEndLine();
			System.out.println("START: " + start);
			System.out.println("END: " + end);
		}
		
	}
	
	

}
