package org.foi.nwtis.bsikac.zadaca_1;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.Charset;

public class KorisnikGlavni {

	public static void main(String[] args) {

		KorisnikGlavni korisnik = new KorisnikGlavni();
		String odgovor = korisnik.posaljiKomandu("localhost", 8001, "AIRPORT");
		korisnik.ispis(odgovor);
	}

	public String posaljiKomandu(String adresa, int port, String komanda) {
		try (Socket veza = new Socket(adresa, port);
				InputStreamReader isr = new InputStreamReader(veza.getInputStream(), Charset.forName("UTF-8"));
				OutputStreamWriter osw = new OutputStreamWriter(veza.getOutputStream(), Charset.forName("UTF-8"));) {

			osw.write(komanda);
			osw.flush();
			veza.shutdownOutput();
			StringBuilder tekst = new StringBuilder();
			int i = isr.read();
			while (i != -1) {
				i = isr.read();
				tekst.append((char) i);
			}
			veza.shutdownInput();
			veza.close();
			return tekst.toString();
		} catch (SocketException e) {
			ispis(e.getMessage());
		} catch (IOException ex) {
			ispis(ex.getMessage());
		}
		return null;
	}

	private void ispis(String message) {
		System.out.println(message);
	}
}
