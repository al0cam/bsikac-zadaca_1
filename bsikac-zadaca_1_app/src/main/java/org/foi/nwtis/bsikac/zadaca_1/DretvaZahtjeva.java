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
	

	public String obradiNaredbu(String zahtjev) {
		Pattern pVelikiRegex = Pattern.compile(
				"^USER (?<user>[A-Za-z0-9_-]{3,10}) PASSWORD (?<password>[A-Za-z0-9_\\-#!]{3,10}) (?<velikaGrupa>(?<aerodrom>AIRPORT((?<aerodromNull>$)| (?<aeroIcao>[A-Z]{4})($|( (?<brojKm>\\d{1,5})$))))|(?<meteo>METEO (?<meteoIcao>[A-Z]{4})($|(?<meteoIcaoDatum> (?<meteoIcaoDatumDatum>\\d{4}\\-\\d{2}\\-\\d{2})$)))|(?<temp>TEMP (?<tempTemp1>-?\\d\\,\\d) (?<tempTemp2>-?\\d\\,\\d)($| (?<tempDatumDatum>\\d{4}\\-\\d{2}\\-\\d{2})$))|(?<serverGlavni>CACHE (?<serverGlavniOpcija>BACKUP|RESTORE|CLEAR|STAT)$)|(?<udaljenost>DISTANCE (?<udaljenostMetode>CLEAR$|(?<udaljenostAerodromOd>[A-Z]{4}) (?<udaljenostAerodromDo>[A-Z]{4})$)))"
				
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
			}else if(komanda.contains("METEO") || komanda.contains("TEMP") )
			{
				odgovor = posaljiKomandu(server.serverMeteoAdresa, server.serverMeteoPort, komanda, server);
			}else if(komanda.contains("CACHE"))
			{
				odgovor = posaljiKomandu(server.serverMeteoAdresa, server.serverMeteoPort, komanda, server);
			}
		}
		return odgovor;
	}
	
	public String cacheBackup()
	{
		String odgovor = "";
		if(server.meduspremnik.pisiUBin(server.datotekaMeduspremnik))
			return "OK";
		return odgovor;
	}

	public String cacheRestore()
	{
		String odgovor = "";
		Meduspremnik m = server.meduspremnik.citajIzBin(server.datotekaMeduspremnik);
		if(m != null)
		{
			server.meduspremnik = m;
			return "OK";
		}
		return odgovor;
	}
	
	public String cacheClear()
	{
		String odgovor = "OK";
		server.meduspremnik.clear();
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

	public boolean ispravanKorisnik(String k, String p)
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
