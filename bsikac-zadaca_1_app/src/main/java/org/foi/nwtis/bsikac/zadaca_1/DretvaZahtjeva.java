package org.foi.nwtis.bsikac.zadaca_1;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.Charset;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.foi.nwtis.bsikac.vjezba_03.konfiguracije.Konfiguracija;

public class DretvaZahtjeva extends Thread {

	private Socket veza = null;
	private Konfiguracija konfig = null;
	private int brojDretve;
	private String ime = "bsikac_";
	private ServerGlavni server;

	public DretvaZahtjeva(Socket veza, Konfiguracija konfig, int brojDretve) {
		super();
		this.veza = veza;
		this.konfig = konfig;
		this.brojDretve = brojDretve;
		setName(ime + brojDretve);

	}

	@Override
	public synchronized void start() {
		// TODO Auto-generated method stub
		super.start();
	}

	@Override
	public void run() {
		try (InputStreamReader isr = new InputStreamReader(this.veza.getInputStream(), Charset.forName("UTF-8"));
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
			System.out.println(tekst.toString()); // TODO kasnije obrisati
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
	
	public void postaviServerGlavni(ServerGlavni server)
	{
		this.server = server;
	}
	

	private String obradiNaredbu(String zahtjev) {
		String auth = "^USER ([A-Za-z0-9_-]{3,10}) PASSWORD ([A-Za-z0-9\\_\\-\\#\\!]{3,10})";
		Pattern pAero = Pattern.compile(auth + " AIRPORT$"), pAeroIcao = Pattern.compile(auth + " AIRPORT ([A-Z]{4})$"),
				pAeroIcaoBroj = Pattern.compile(auth + " AIRPORT ([A-Z]{4}) (\\d{1,7})$");

		Pattern pDist = Pattern.compile(auth + " DISTANCE ([A-Z]{4}) ([A-Z]{4})$"),
				pDistClear = Pattern.compile(auth + " DISTANCE CLEAR$");

		Pattern pMeteoIcao = Pattern.compile(auth + " METEO ([A-Z]{4})$"),
				pMeteoIcaoDatum = Pattern.compile(auth + " METEO ([A-Z]{4}) (\\d{4}-\\d{2}-\\d{2})$"),
				pTemp = Pattern.compile(auth + " TEMP (\\d,\\d) (\\d,\\d)$"),
				pTempDatum = Pattern.compile(auth + " TEMP (\\d,\\d) (\\d,\\d) (\\d{4}-\\d{2}-\\d{2})$");

		Matcher mAero = pAero.matcher(zahtjev), mAeroIcao = pAeroIcao.matcher(zahtjev),
				mAeroIcaoBroj = pAeroIcaoBroj.matcher(zahtjev);

		Matcher mDist = pDist.matcher(zahtjev), mDistClear = pDistClear.matcher(zahtjev);

		Matcher mMeteoIcao = pMeteoIcao.matcher(zahtjev), mMeteoIcaoDatum = pMeteoIcaoDatum.matcher(zahtjev),
				mTemp = pTemp.matcher(zahtjev), mTempDatum = pTempDatum.matcher(zahtjev);
		
		String odgovor = "ERROR 40 Format komande nije ispravan";
		
		if (mAero.matches()) {
			odgovor = posaljiKomandu(server.serverAerodromaAdresa, server.serverAerodromaPort, zahtjev);
		} else if (mAeroIcao.matches()) {
			odgovor = posaljiKomandu(server.serverAerodromaAdresa, server.serverAerodromaPort, zahtjev);
		} else if (mAeroIcaoBroj.matches()) {
			odgovor = posaljiKomandu(server.serverAerodromaAdresa, server.serverAerodromaPort, zahtjev);
		}else if (mDist.matches()) {
			odgovor = posaljiKomandu(server.serverUdaljenostiAdresa, server.serverUdaljenostiPort,zahtjev);
		} else if (mDistClear.matches()) {
			odgovor = posaljiKomandu(server.serverUdaljenostiAdresa, server.serverUdaljenostiPort, zahtjev);
		} else if(mMeteoIcao.matches()) 
		{
			odgovor = posaljiKomandu(server.serverMeteoAdresa, server.serverMeteoPort,zahtjev);
		} else if(mMeteoIcaoDatum.matches()) {
			odgovor = posaljiKomandu(server.serverMeteoAdresa, server.serverMeteoPort,zahtjev);
		}
		else if(mTemp.matches())
		{
			odgovor = posaljiKomandu(server.serverMeteoAdresa, server.serverMeteoPort,zahtjev);
		}
		else if(mTempDatum.matches())
		{
			odgovor = posaljiKomandu(server.serverMeteoAdresa, server.serverMeteoPort,zahtjev);
		}
		

		return odgovor;
	}

	public String posaljiKomandu(String adresa, int port, String komanda) {
		try (Socket veza = new Socket(adresa, port);
				InputStreamReader isr = new InputStreamReader(veza.getInputStream(), Charset.forName("UTF-8"));
				OutputStreamWriter osw = new OutputStreamWriter(veza.getOutputStream(), Charset.forName("UTF-8"));) {

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
