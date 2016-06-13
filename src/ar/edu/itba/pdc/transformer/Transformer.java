package ar.edu.itba.pdc.transformer;

import java.io.FileNotFoundException;
import java.io.IOException;

import ar.edu.itba.pdc.mail.Mail;

public interface Transformer {
	public void transform(Mail mail) throws FileNotFoundException, IOException;
}