package ar.edu.itba.pdc.mail;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;


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
		
		// Muestro resultados
		System.out.println("FROM: " + mail.getFrom());
		System.out.println("DATE: " + mail.getDate());
		System.out.println("SIZE: " + mail.getCurrentSize());
		System.out.println("HEADERS: " + mail.getHeader());
		System.out.println("BODY START: " + mail.getBodyStartIndex());
		System.out.println("BODY END: " + mail.getBodyEndIndex());
		System.out.println("HTML START: " + mail.getHtmlStartIndex());
		System.out.println("HTML END: " + mail.getHtmlEndIndex());
		System.out.println("IMAGES COUNT: " + mail.getMailImages().size());
		
	}
	
	

}
