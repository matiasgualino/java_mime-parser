package ar.edu.itba.pdc.mail;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Mail {

	public static final String DIRECTORY_NAME = "mails";
	
	// Constante para cada linea. Carriage Return & Linefeed
	public static final String CR_LF = "\r\n";
	private static final String BOUNDARY_TOKEN = "boundary";
	
	enum STATES {FROM, DATE, SUBJECT, TO, CONTENT_TYPE, DEFAULT}
	enum CONTENT_TYPES {TEXT, MULTIPART, IMAGE, HTML, NONE}
	
	// Constantes utiles para parsear los headers
	private static final String FROM = "From:";
	private static final String DATE = "Date: ";
	private static final String SUBJECT = "Subject:";
	private static final String TO = "To:";
	
	// ContentType section
	private static final String CONTENTTYPE = "Content-Type: ";
	private static final String TEXT = "Content-Type: text/plain";
	private static final String MULTIPART = "Content-Type: multipart";
	private static final String IMAGE = "Content-Type: image";
	private static final String HTML = "Content-Type: text/html";
	
	private static final String CONTENTDISP = "Content-Disposition: ";
	
	private static final String Q_PRINT = "Content-Transfer-Encoding: quoted-printable";
	
	// Datos provenientes de los headers
	
	// Quien envió el mail
	private String from;
	// Fecha de envío del mail
	private String date;
	// Asunto del mail
	private String subject;
	// Quien recibe el mail
	private String to;
	// Header del mail
	private String header;
	// ContentTypes del mail
	private Set<String> contentTypes = new HashSet<String>();
	// ContentDispositions del mail
	private Set<String> contentDispositions = new HashSet<String>();
	
	private Set<String> mailBounds = new HashSet<String>();
	private boolean quotedPrint;
	private int subjectStartIndex, subjectEndIndex;
	private List<MailImage> mailImages = new ArrayList<MailImage>();
	
	private boolean parsingSubject = false;
	
	// Cantidad de mails leídos hasta el momento.
	private static int CANT_MAILS;
	// Maxima cantidad de mails que vamos a leer.
	private static int MAX_MAILS_SUPPORTED = 1000;
	// Identificador del mail actual.
	private int number;
	// Tamanio del mail (cantidad de caracteres).
	private int currentSize;
	private int linesRead = 0;
	private RandomAccessFile mailReader, mailWriter;
	
	public Mail() throws IOException {
		// Creo identificador basado en la cantidad de mails actuales.
		this.number = (CANT_MAILS++)%MAX_MAILS_SUPPORTED;
		// Creo el archivo para el mail e inicializo el reader y writer
		this.createMailFile();
	}
	
	private void createDirectoryIfNotExists(String directoryName) {
		File mailsDirectory = new File(directoryName);
        if(!mailsDirectory.exists()) {
            mailsDirectory.mkdir();
        }
	}
	
	private void createMailFile() throws IOException {
		// Creo un directorio para los mails
		this.createDirectoryIfNotExists(DIRECTORY_NAME);
		// Creo un archivo para el mail. 
		File mailFile = new File(DIRECTORY_NAME + "/" + this.number + ".txt");
		// Lo elimino por si ya supere la cantidad de mails.
		mailFile.delete();
		mailFile.createNewFile();
		// Cuando ya tengo el archivo creado, tengo un acceso para leerlo y otro para escribirlo.
		this.mailReader = new RandomAccessFile(DIRECTORY_NAME + "/" + this.number + ".txt", "r");
		this.mailWriter = new RandomAccessFile(DIRECTORY_NAME + "/" + this.number + ".txt", "rw");
	}
	
	public void addLine(String line) throws IOException {
		currentSize += line.length();
		mailWriter.write((line).getBytes());
	}
	
	public void parse() throws IOException {
		// Si voy a parsear, no permito que escriban mas.
		this.mailWriter.close();
		// Datos para la lectura
		String currentLine;
		STATES currentState;
		STATES lastState = STATES.DEFAULT;
		// Pero si voy a leer uso mailReader
		while((currentLine = this.mailReader.readLine()) != null) {
			addHeader(currentLine);
			currentState = getState(currentLine);
			
			switch(currentState) {
				case FROM:
					solveFromState(currentLine);
					break;
				case DATE:
					solveDateState(currentLine);
					break;
				case SUBJECT:
					solveSubjectState(currentLine);
					break;
				case TO:
					solveToState(currentLine);
					break;
				case CONTENT_TYPE:
					solveContentTypeState(currentLine);
					break;
				default:
					if (parsingSubject) {
						subject += currentLine + CR_LF;
						this.subjectEndIndex = this.linesRead;
					}
					break;
			}
			lastState = currentState;
			this.linesRead++;
		}
		this.mailReader.close();
	}
	
	private STATES getState(String line) {
		if(line.toLowerCase().startsWith(FROM.toLowerCase())) {
			return STATES.FROM;
		} else if(line.toLowerCase().startsWith(DATE.toLowerCase())) {
			return STATES.DATE;
		} else if(line.toLowerCase().startsWith(SUBJECT.toLowerCase())) {
			return STATES.SUBJECT;
		} else if(line.toLowerCase().startsWith(TO.toLowerCase())) {
			return STATES.TO;
		} else if(line.toLowerCase().startsWith(CONTENTTYPE.toLowerCase())) {
			return STATES.CONTENT_TYPE;
		}
		return STATES.DEFAULT;
	}
	
	private CONTENT_TYPES getContentType(String line) {
		if(line.toLowerCase().startsWith(MULTIPART.toLowerCase())) {
			return CONTENT_TYPES.MULTIPART;
		} else if(line.toLowerCase().startsWith(TEXT.toLowerCase())) {
			return CONTENT_TYPES.TEXT;
		} else if(line.toLowerCase().startsWith(IMAGE.toLowerCase())) {
			return CONTENT_TYPES.IMAGE;
		} else if(line.toLowerCase().startsWith(HTML.toLowerCase())) {
			return CONTENT_TYPES.HTML;
		}
		return CONTENT_TYPES.NONE;
	}
	
	private void solveFromState(String line) {
		this.parsingSubject = false;
		this.from = line.split(FROM)[1];
	}
	
	private void solveToState(String line) {
		this.parsingSubject = false;
		this.to = line.split(TO)[1];
	}
	
	private void solveSubjectState(String line) {
		if (this.subject == null) {
			this.subject = "";
			parsingSubject = true;
			this.subjectStartIndex = this.linesRead;
		}
		this.subject += line.split(SUBJECT)[1] + CR_LF;
	}
	
	private void solveDateState(String line) {
		// Date: Thu, 9 Jun 2016 13:54:30 -0300
		//  0     1   2  3    4
		this.parsingSubject = false;
		String[] dateArray = line.split(" ");
		this.date = dateArray[2] + "/" + getMonth(dateArray[3]) + "/" + dateArray[4];
	}
	
	private void solveContentTypeState(String line) throws IOException {
		this.parsingSubject = false;
		this.contentTypes.add(line.split(CONTENTTYPE)[1]);
		CONTENT_TYPES currentType = getContentType(line);
		
		switch(currentType) {
			case MULTIPART:
				solveMultipartType(line);
				break;
			case TEXT:
				solveTextType(line);
				break;
			case IMAGE:
				solveImageType();
				break;
			case HTML:
				solveHTMLType(line);
				break;
			case NONE:
				solveNoneType(line);
				break;
			default:
				break;
		}
	}
	
	private void solveMultipartType(String line) throws IOException {
		String bound;
		while(line != null && !line.contains(BOUNDARY_TOKEN)) {
			addHeader(line);
			this.linesRead++;
			line = this.mailReader.readLine();
		}
		addHeader(line);
		bound = line.split(BOUNDARY_TOKEN + "=")[1];
		if(bound.contains("\"")) {
			bound = bound.split("\"")[1];
		}
		mailBounds.add(bound);
	}
	
	private void solveTextType(String line) throws IOException {
		consumeHeader(line);
		readToBoundFound();
	}
	
	private void solveHTMLType(String line) throws IOException {
		consumeHeader(line);
		addHeader(line);
		this.linesRead++;
		readToBoundFound();
	}
	
	private void solveImageType() throws IOException {
		this.linesRead++;
		String line = this.mailReader.readLine();
		consumeHeader(line);
		this.linesRead++;
		line = this.mailReader.readLine();
		
		MailImage image = new MailImage();
		image.setStartLine(this.linesRead);

		while(line != null && !line.contains("--") && !line.equals("")) {
			this.linesRead++;
			line = this.mailReader.readLine();
		}
		
		image.setEndLine(this.linesRead);
		mailImages.add(image);
	}
	
	private void solveNoneType(String line) throws IOException {
		consumeHeader(line);
		
		while(line != null && line.equals("")) {
			line = this.mailReader.readLine();
		}
		
		boolean found = false;
		while(!found && line != null && !line.equals("")) {
			for(String bound : this.mailBounds) {
				if(line.startsWith("--" + bound) || line.equals(bound)) {
					found = true;
					break;
				}
			}
			this.linesRead++;
			line = this.mailReader.readLine();
		}
	}
	
	private void consumeHeader(String line) throws IOException {
		while(line != null && !line.equals("")) {
			setIfQuotedPrint(line);
			addContentDisposition(line);
			addHeader(line);
			this.linesRead++;
			line = this.mailReader.readLine();
		}
		addHeader(line);
	}
	
	private void readToBoundFound() throws IOException {
		String line = this.mailReader.readLine();
		boolean found = false;
		while(!found && line != null && !line.equals("--=20")) {
			for(String bound : this.mailBounds) {
				if(line.startsWith("--" + bound) || line.equals(bound)) {
					found = true;
					break;
				}
			}
			if(!found) {
				this.linesRead++;
				line = this.mailReader.readLine();
			}
		}
	}
	
	private void setIfQuotedPrint(String line) {
		if(line.toLowerCase().contains(Q_PRINT.toLowerCase())) {
			this.quotedPrint = true;
		}
	}
	
	private void addHeader(String line) {
		if (this.header == null) {
			this.header = "";
		}
		this.header += line + CR_LF;
	}
	
	private void addContentDisposition(String line) {
		if(line.toLowerCase().startsWith(CONTENTDISP.toLowerCase())) {
			String disp = line.split(CONTENTDISP)[1];
			disp = disp.split(";")[0];
			contentDispositions.add(disp);
		}
	}
	
	private String getMonth(String monthNamePrefix) {
		if(monthNamePrefix.equals("Jan")) {
			return "01";
		} else if(monthNamePrefix.equals("Feb")) {
			return "02";
		} else if(monthNamePrefix.equals("Mar")) {
			return "03";
		} else if(monthNamePrefix.equals("Apr")) {
			return "04";
		} else if(monthNamePrefix.equals("May")) {
			return "05";
		} else if(monthNamePrefix.equals("Jun")) {
			return "06";
		} else if(monthNamePrefix.equals("Jul")) {
			return "07";
		} else if(monthNamePrefix.equals("Aug")) {
			return "08";
		} else if(monthNamePrefix.equals("Sep")) {
			return "09";
		} else if(monthNamePrefix.equals("Oct")) {
			return "10";
		} else if(monthNamePrefix.equals("Nov")) {
			return "11";
		} else if(monthNamePrefix.equals("Dec")) {
			return "12";
		} else {
			return null;
		}
	}
	
	public boolean hasHeader(String headerItem) {
		return this.header.toLowerCase().contains(headerItem.toLowerCase());
	}
	
	public boolean isQuotedPrint() {
		return this.quotedPrint;
	}
	
	public String getDate() {
		return this.date;
	}
	
	public String getFrom() {
		return this.from;
	}
	
	public String getSubject() {
		return subject;
	}
	
	public String getHeader() {
		return header;
	}
	
	public int getSubjectStartIndex() {
		return subjectStartIndex;
	}
	
	public int getSubjectEndIndex() {
		return subjectEndIndex;
	}
	
	public List<MailImage> getMailImages() {
		return mailImages;
	}
	
	public Set<String> getContentTypes() {
		return contentTypes;
	}
	
	public int getCurrentSize() {
		return currentSize;
	}
	
	public int getNumber() {
		return number;
	}
	
	public String getTo() {
		return to;
	}
}
