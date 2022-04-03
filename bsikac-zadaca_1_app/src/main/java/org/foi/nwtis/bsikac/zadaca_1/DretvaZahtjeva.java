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
		Pattern pVelikiRegex = Pattern.compile(
"^USER (?<user>[A-Za-z0-9_-]{3,10}) PASSWORD (?<password>[A-Za-z0-9_\\-#!]{3,10}) (?<velikaGrupa>(?<aerodrom>AIRPORT((?<aerodromNull>$)| (?<aeroIcao>[A-Z]{4})($|( (?<brojKm>\\d{1,5})$))))|(?<meteo>METEO (?<meteoIcao>[A-Z]{4})($|(?<meteoIcaoDatum> (?<meteoIcaoDatumDatum>\\d{2}\\.\\d{2}\\.\\d{4}\\.)$)))|(?<temp>TEMP (?<tempTemp1>-?\\d\\,\\d) (?<tempTemp2>-?\\d\\,\\d)($| (?<tempDatumDatum>\\d{2}\\.\\d{2}\\.\\d{4}\\.)$))|(?<serverGlavni>CACHE (?<serverGlavniOpcija>BACKUP|RESTORE|CLEAR|STAT)$)|(?<udaljenost>DISTANCE (?<udaljenostMetode>CLEAR$|(?<udaljenostAerodromOd>[A-Z]{4}) (?<udaljenostAerodromDo>[A-Z]{4})$)))"
				);
		
		Matcher mVelikiRegex = pVelikiRegex.matcher(zahtjev);
		String odgovor = "ERROR 40 Format komande nije ispravan";
		
		if(mVelikiRegex.matches()) {
			String user = mVelikiRegex.group("user");
			String password = mVelikiRegex.group("password");
			String komanda = mVelikiRegex.group("velikaGrupa");
			
			if(!ispravanKorisnik(user, password))
				return "ERROR 41 Korisnik ili lozinka nisu ispravni";
			
			if(komanda.contains("AIRPORT"))
			{
				String o = server.meduspremnik.pronadji(komanda);
				if(o != null) 
					return o;
				odgovor = posaljiKomandu(server.serverAerodromaAdresa, server.serverAerodromaPort, komanda, server);
				server.meduspremnik.dodaj(komanda, odgovor);
			} else if(komanda.contains("DISTANCE"))
			{
				String o = server.meduspremnik.pronadji(komanda);
				if(o != null) 
					return o;
				odgovor = posaljiKomandu(server.serverUdaljenostiAdresa, server.serverUdaljenostiPort, komanda, server);
				server.meduspremnik.dodaj(komanda, odgovor);
			}else if(komanda.contains("METEO"))
			{
				odgovor = posaljiKomandu(server.serverMeteoAdresa, server.serverMeteoPort, komanda, server);
			}else if(komanda.contains("CACHE"))
			{
				odgovor = posaljiKomandu(server.serverMeteoAdresa, server.serverMeteoPort, komanda, server);
			}
		}
		
		String auth = "^USER (?<user>[A-Za-z0-9_-]{3,10}) PASSWORD (?<password>[A-Za-z0-9_\\-#!]{3,10}) (?<zahtjev>";
		Pattern pAero = Pattern.compile(auth + "AIRPORT)$"), pAeroIcao = Pattern.compile(auth + "AIRPORT ([A-Z]{4}))$"),
				pAeroIcaoBroj = Pattern.compile(auth + "AIRPORT ([A-Z]{4}) (\\d{1,7}))$");

		Pattern pDist = Pattern.compile(auth + "DISTANCE ([A-Z]{4}) ([A-Z]{4}))$"),
				pDistClear = Pattern.compile(auth + "DISTANCE CLEAR)$");

		Pattern pMeteoIcao = Pattern.compile(auth + "METEO ([A-Z]{4}))$"),
				pMeteoIcaoDatum = Pattern.compile(auth + "METEO ([A-Z]{4}) (\\d{4}-\\d{2}-\\d{2}))$"),
				pTemp = Pattern.compile(auth + "TEMP (\\d,\\d) (\\d,\\d))$"),
				pTempDatum = Pattern.compile(auth + "TEMP (\\d,\\d) (\\d,\\d) (\\d{4}-\\d{2}-\\d{2}))$");

		Matcher mAero = pAero.matcher(zahtjev), mAeroIcao = pAeroIcao.matcher(zahtjev),
				mAeroIcaoBroj = pAeroIcaoBroj.matcher(zahtjev);

		Matcher mDist = pDist.matcher(zahtjev), mDistClear = pDistClear.matcher(zahtjev);

		Matcher mMeteoIcao = pMeteoIcao.matcher(zahtjev), mMeteoIcaoDatum = pMeteoIcaoDatum.matcher(zahtjev),
				mTemp = pTemp.matcher(zahtjev), mTempDatum = pTempDatum.matcher(zahtjev);
				
		String odgovor = "ERROR 40 Format komande nije ispravan";
		
		if (mAero.matches()) {
			String z = mAero.group("zahtjev");
			String o = server.meduspremnik.pronadji(z);
			if(o != null) 
				return o;
			odgovor = posaljiKomandu(server.serverAerodromaAdresa, server.serverAerodromaPort, z, mAero.group("user"), mAero.group("password"), server);
			server.meduspremnik.dodaj(z, odgovor);
		} else if (mAeroIcao.matches()) {
			String z = mAeroIcao.group("zahtjev");
			String o = server.meduspremnik.pronadji(z);
			if(o != null) 
				return o;
			odgovor = posaljiKomandu(server.serverAerodromaAdresa, server.serverAerodromaPort, z, mAeroIcao.group("user"), mAeroIcao.group("password"), server);
			server.meduspremnik.dodaj(z, odgovor);
		} else if (mAeroIcaoBroj.matches()) {
			String z = mAeroIcaoBroj.group("zahtjev");
			String o = server.meduspremnik.pronadji(z);
			if(o != null) 
				return o;
			odgovor = posaljiKomandu(server.serverAerodromaAdresa, server.serverAerodromaPort, z, mAeroIcaoBroj.group("user"), mAeroIcaoBroj.group("password"), server);
			server.meduspremnik.dodaj(z, odgovor);
		}else if (mDist.matches()) {
			String z = mDist.group("zahtjev");
			String o = server.meduspremnik.pronadji(z);
			if(o != null) 
				return o;
			odgovor = posaljiKomandu(server.serverUdaljenostiAdresa, server.serverUdaljenostiPort,z, mDist.group("user"), mDist.group("password"), server);
			server.meduspremnik.dodaj(z, odgovor);
		} else if (mDistClear.matches()) {
			String z = mDistClear.group("zahtjev");
			String o = server.meduspremnik.pronadji(z);
			if(o != null) 
				return o;
			odgovor = posaljiKomandu(server.serverUdaljenostiAdresa, server.serverUdaljenostiPort, z, mDistClear.group("user"), mDistClear.group("password"), server);
			server.meduspremnik.dodaj(z, odgovor);
		} else if(mMeteoIcao.matches()) {
			String z = mMeteoIcao.group("zahtjev");
			String o = server.meduspremnik.pronadji(z);
			if(o != null) 
				return o;
			odgovor = posaljiKomandu(server.serverMeteoAdresa, server.serverMeteoPort, z, mMeteoIcao.group("user"), mMeteoIcao.group("password"), server);
		} else if(mMeteoIcaoDatum.matches()) {
			String z = mMeteoIcaoDatum.group("zahtjev");
			String o = server.meduspremnik.pronadji(z);
			if(o != null) 
				return o;
			odgovor = posaljiKomandu(server.serverMeteoAdresa, server.serverMeteoPort, z, mMeteoIcaoDatum.group("user"), mMeteoIcaoDatum.group("password"), server);
		} else if(mTemp.matches()) {
			String z = mTemp.group("zahtjev");
			String o = server.meduspremnik.pronadji(z);
			if(o != null) 
				return o;
			odgovor = posaljiKomandu(server.serverMeteoAdresa, server.serverMeteoPort, z, mTemp.group("user"), mTemp.group("password"), server);
			server.meduspremnik.dodaj(z, odgovor);
		} else if(mTempDatum.matches()) {
			String z = mTempDatum.group("zahtjev");
			String o = server.meduspremnik.pronadji(z);
			if(o != null) 
				return o;
			odgovor = posaljiKomandu(server.serverMeteoAdresa, server.serverMeteoPort, z, mTempDatum.group("user"), mTempDatum.group("password"), server);
			server.meduspremnik.dodaj(z, odgovor);
		}
		
		return odgovor;
	}

	public String posaljiKomandu(String adresa, int port, String komanda, ServerGlavni server) {
		
		
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
			System.out.println(tekst.toString());
			veza.shutdownInput();
			veza.close();
			return tekst.toString();
		} catch (SocketException e) {
			ispis(e.getMessage());
		} catch (IOException ex) {
			ispis(ex.getMessage());
		}
		
		if(server.serverAerodromaPort == port)
			return "ERROR 43 server aerodroma ne radi";
		else if(server.serverMeteoPort == port)
			return "ERROR 42 server meteo ne radi";
		else if(server.serverUdaljenostiPort == port)
			return "ERROR 44 server udaljenosti ne radi";
		else return null;
	}

	private boolean ispravanKorisnik(String k, String p)
	{
		for(Korisnik korisnik: server.getKorisnici())
		{
			if(korisnik.getKorisnickoIme().equals(k) && korisnik.getLozinka().equals(p))
				return true;
		}
		return false;
	}
	
	private void ispis(String message) {
		System.out.println(message);
	}
	
	

}
