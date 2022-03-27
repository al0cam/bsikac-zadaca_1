package org.foi.nwtis.bsikac.zadaca_1;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.foi.nwtis.bsikac.vjezba_03.konfiguracije.Konfiguracija;
import org.foi.nwtis.bsikac.vjezba_03.konfiguracije.KonfiguracijaApstraktna;
import org.foi.nwtis.bsikac.vjezba_03.konfiguracije.NeispravnaKonfiguracija;

public class ServerAerodroma {
	private int port = 0;
	private int maksCekaca = -1;
	private Socket veza = null;
	private List<AerodromMeteo> meteoPodaci = new ArrayList<AerodromMeteo>();
	
	private static SimpleDateFormat isoDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
	private static Konfiguracija konfig = null;
	
	public static void main(String[] args) {
		if(args.length != 1) {
			System.out.println("Parametar mora biti naziv konfiguracijske datoteke!");
			return;
		}
	
		if(!ucitajKonfiguraciju(args[0])) return;
		
		//TODO provjeri jesu li sve postavke koje trebaju biti
		if(!konfiguracijaSadrzi("port")) return;
		if(!konfiguracijaSadrzi("maks.cekaca")) return;
		if(!konfiguracijaSadrzi("datoteka.meteo")) return;
			
		
		int port = Integer.parseInt(konfig.dajPostavku("port"));
		if(port < 8000 || port > 9999)
		{
			System.out.println("Port: "+port+ " nije u dozvoljenom rasponu(8000-9999)");
			return;
		}
		if(!portSlobodan(port)) return;
		
		int maksCekaca = Integer.parseInt(konfig.dajPostavku("maks.cekaca"));
		String nazivDatotekeMeteoPodataka = konfig.dajPostavku("datoteka.meteo");
		
		ServerAerodroma sm = new ServerAerodroma(port, maksCekaca);
		if(!sm.ucitajMeteoPodatke(nazivDatotekeMeteoPodataka)) return;
		
		System.out.println("Server se podiže na portu: "+port);
		sm.obradaZahtjeva();
	}

	public ServerAerodroma(int port, int maksCekaca) {
		this.port = port;
		this.maksCekaca = maksCekaca;
	}
	
	private static boolean ucitajKonfiguraciju(String nazivDatoteke) {
		try {
			konfig = KonfiguracijaApstraktna.preuzmiKonfiguraciju(nazivDatoteke);
		} catch (NeispravnaKonfiguracija e) {
			// TODO Javi nešto pametno
			System.out.println("Došlo je do pogreške prilikom učitavanja konfiguracije!");
			e.printStackTrace();
			return false;
		}
		return true;
	}
		
	private boolean ucitajMeteoPodatke(String nazivDatotekeMeteoPodataka) {
		try {
			
			FileReader fr = new FileReader(nazivDatotekeMeteoPodataka,Charset.forName("UTF-8"));
			BufferedReader br = new BufferedReader(fr);
			while(true) {
				String linija = br.readLine();
				if(linija==null || linija.isEmpty()) break;
				//TODO razmisli o mogućim problemima kod učitavanja
				String[] p = linija.split(";");
				AerodromMeteo am = new AerodromMeteo(p[0],Double.parseDouble(p[1]),
							Double.parseDouble(p[2]),Double.parseDouble(p[3]),p[4],
							isoDateFormat.parse(p[4]).getTime());
				meteoPodaci.add(am);
				//System.out.println(linija);
			}
			System.out.println("Učitano " + meteoPodaci.size() + " meteo podataka!");
		} catch (IOException | NumberFormatException | ParseException e) {
			if(e.getMessage().contains("Permission denied"))
				System.out.println("Nije omoguceno citanje datoteke u pravima pristupa!");
			else
				System.out.println("Datoteka ne postoji!");
			return false;
		}		
		return true;
	}

	public void obradaZahtjeva() {

		try (ServerSocket ss = new ServerSocket(this.port, this.maksCekaca)) {			
			while (true) {
				System.out.println("Čekam korisnika!"); //TODO kasnije obrisati
				this.veza = ss.accept();

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
					Thread.sleep(10000);
					osw.write("Odgovor: "+odgovor);
					osw.flush();
					this.veza.shutdownOutput();
				} catch (SocketException | InterruptedException e) {
					e.printStackTrace();
				}
			}

		} catch (IOException ex) {
			Logger.getLogger(ServerAerodroma.class.getName()).log(Level.SEVERE, null, ex);
		}

	}

	private String obradiNaredbu(String zahtjev) {
		Pattern pAero = Pattern.compile("^AIRPORT$");
		Pattern pAeroIcao = Pattern.compile("^AIRPORT ([A-Z]{4})$");
		Pattern pAeroIcaoBroj = Pattern.compile("^AIRPORT ([A-Z]{4}) (\\d{1,7})$");

		Matcher mAero = pAero.matcher(zahtjev);
		Matcher mAeroIcao = pAeroIcao.matcher(zahtjev);
		Matcher mAeroIcaoBroj = pAeroIcaoBroj.matcher(zahtjev);

		String odgovor = "ERROR 20 Format komande nije ispravan";
		
		if(mAero.matches()) 
		{
			odgovor = izvrsiNaredbuAero(zahtjev);
		} else if(mAeroIcao.matches()) {
			odgovor = izvrsiNaredbuAeroIcao(zahtjev);
		}
		else if(mAeroIcaoBroj.matches())
		{
			odgovor = izvrsiNaredbuAeroIcaoBroj(zahtjev);
		}
		
		return odgovor;
	}

	private String izvrsiNaredbuAero(String zahtjev) {
		String[] podaci = zahtjev.split(" ");
		String icao = podaci[1];
		String popisRezultata = "";
		for (AerodromMeteo am : meteoPodaci) {
			if(popisRezultata.length() <= 0)
			{
				popisRezultata = "OK "+am.icao+ ";";
			}
			else
			{
				popisRezultata = popisRezultata.concat(" "+am.icao +";");
			}
			
		}
		if(popisRezultata.length() <= 0)
			popisRezultata = "ERROR 11 Aerodrom '"+icao+"' ne postoji!";
			
		return popisRezultata;
	}
	
	private String izvrsiNaredbuAeroIcao(String zahtjev) {
		String[] podaci = zahtjev.split(" ");
		String icao = podaci[1];
		String datum = podaci[2];
		String popisRezultata = "";
		for (AerodromMeteo am : meteoPodaci) {
			
			if(am.icao.equals(icao) && am.vrijeme.contains(datum)) 
			{
				if(popisRezultata.length() <= 0)
				{
					popisRezultata = "OK "+am.temp+" "+am.vlaga+" "+am.tlak+" "+am.vrijeme+";";
				}
				else
				{
					popisRezultata = popisRezultata.concat(" "+am.temp+" "+am.vlaga+" "+am.tlak+" "+am.vrijeme+";");
				}
			}
		}
		if(popisRezultata.length() <= 0)
			popisRezultata = "ERROR 11 Aerodrom '"+icao+"' ne postoji!";
			
		return popisRezultata;
//		return "ERROR 11 Aerodrom '"+icao+"' ne postoji!";//TODO provjeri opis zadaće
	}
	
	private String izvrsiNaredbuAeroIcaoBroj(String zahtjev) {
		String[] podaci = zahtjev.split(" ");
		double temp1 = Double.parseDouble(podaci[1].replace(',', '.'));
		double temp2 = Double.parseDouble(podaci[2].replace(',', '.'));
		String popisRezultata = "";
		for (AerodromMeteo am : meteoPodaci) {
			if(am.temp >= temp1 && am.temp <= temp2) 
			{
				if(popisRezultata.length() <= 0)
				{
					popisRezultata = "OK "+am.icao+" "+am.temp+" "+am.vlaga+" "+am.tlak+" "+am.vrijeme+";";
				}
				else
				{
					popisRezultata = popisRezultata.concat(" "+am.icao+" "+am.temp+" "+am.vlaga+" "+am.tlak+" "+am.vrijeme+";");
				}
			}
		}
		if(popisRezultata.length() <= 0)
			popisRezultata = "ERROR 11 ne postoje meteo podaci s temepraturom u rasponu od '"+temp1+"' do '"+temp2+"'!";
			
		return popisRezultata;
//		return "ERROR 11 Aerodrom '"+icao+"' ne postoji!";//TODO provjeri opis zadaće
	}
	
	
	
	
	private static boolean konfiguracijaSadrzi(String kljuc)
	{
		if(konfig.dajPostavku(kljuc)==null || konfig.dajPostavku(kljuc).isEmpty())
		{
			System.out.println(kljuc+" nije definiran u konfiguraciji!");
			return false;
		}
		return true;
		
	}
	
	private static boolean portSlobodan(int port)
	{
		ServerSocket skt;
		try {
			skt = new ServerSocket(port);
			skt.close();
		} catch (IOException e ) {
			// TODO Auto-generated catch block
//			e.printStackTrace();
			System.out.println("Port se vec koristi!");
			return false;
		}
		return true;
		
	}
	
	
}
