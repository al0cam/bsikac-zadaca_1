package org.foi.nwtis.bsikac.zadaca_1;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.Charset;

import org.foi.nwtis.bsikac.vjezba_03.konfiguracije.Konfiguracija;

public class DretvaZahtjeva extends Thread {

	private Socket veza = null;
	private Konfiguracija konfig = null;

	public DretvaZahtjeva(Socket veza, Konfiguracija konfig) {
		super();
		this.veza = veza;
		this.konfig = konfig;
	}

	@Override
	public synchronized void start() {
		// TODO Auto-generated method stub
		super.start();
	}

	@Override
	public void run() {
		try (InputStreamReader isr = new InputStreamReader(this.veza.getInputStream(),
				Charset.forName("UTF-8"));
				OutputStreamWriter osw = new OutputStreamWriter(this.veza.getOutputStream(),
						Charset.forName("UTF-8"));) {

			StringBuilder tekst = new StringBuilder();
			while (true) {
				int i = isr.read();
				if (i == -1) {
					break;
				}
				tekst.append((char) i);
			}
			System.out.println(tekst.toString()); //TODO kasnije obrisati
			this.veza.shutdownInput();
				
			String odgovor = obradiNaredbu(tekst.toString()); 
//			sleep(10000);
			osw.write(odgovor);
			osw.flush();
			this.veza.shutdownOutput();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void interrupt() {
		// TODO Auto-generated method stub
		super.interrupt();
	}

	private String obradiNaredbu(String zahtjev) {
		String odgovor = posaljiKomandu(
				konfig.dajPostavku("server.meteo.adresa"), 
				Integer.parseInt(konfig.dajPostavku("server.meteo.port")), 
				zahtjev.toString());
		return odgovor;
	}
	
	 public String posaljiKomandu(String adresa, int port, String komanda) {
	        try (
	                 Socket veza = new Socket(adresa, port);
	                 InputStreamReader isr = new InputStreamReader(veza.getInputStream(),
	                        Charset.forName("UTF-8"));
	                 OutputStreamWriter osw = new OutputStreamWriter(veza.getOutputStream(),
	                        Charset.forName("UTF-8"));) {  

	            osw.write(komanda);
	            osw.flush();
	            veza.shutdownOutput();
	            StringBuilder tekst = new StringBuilder();
	            while (true) {
	                int i = isr.read();
	                if (i == -1) {
	                    break;
	                }
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
