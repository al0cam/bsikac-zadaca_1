package org.foi.nwtis.bsikac.zadaca_1;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
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

public class ServerMeteo {
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
		
		int port = Integer.parseInt(konfig.dajPostavku("port"));
		int maksCekaca = Integer.parseInt(konfig.dajPostavku("maks.cekaca"));
		String nazivDatotekeMeteoPodataka = konfig.dajPostavku("datoteka.meteo");
		System.out.println("Server se podiže na portu: "+port);
		
		ServerMeteo sm = new ServerMeteo(port,maksCekaca);
		sm.ucitajMeteoPodatke(nazivDatotekeMeteoPodataka);
		sm.obradaZahtjeva();
	}

	public ServerMeteo(int port, int maksCekaca) {
		this.port = port;
		this.maksCekaca = maksCekaca;
	}
	
	private static boolean ucitajKonfiguraciju(String nazivDatoteke) {
		try {
			konfig = KonfiguracijaApstraktna.preuzmiKonfiguraciju(nazivDatoteke);
		} catch (NeispravnaKonfiguracija e) {
			// TODO Javi nešto pametno
			e.printStackTrace();
			return false;
		}
		return true;
	}
		
	private void ucitajMeteoPodatke(String nazivDatotekeMeteoPodataka) {
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
			System.out.println("Učitano "+meteoPodaci.size()+" meteo podataka!");
		} catch (IOException | NumberFormatException | ParseException e) {
			// TODO Napiši nešto pametno
			e.printStackTrace();
		}		
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
			Logger.getLogger(ServerMeteo.class.getName()).log(Level.SEVERE, null, ex);
		}

	}

	private String obradiNaredbu(String zahtjev) {
		Pattern pMeteoIcao = Pattern.compile("^METEO ([A-Z]{4})$");
		Pattern pMeteoIcaoDatum = Pattern.compile("^METEO ([A-Z]{4}) (\\d{4}-\\d{2}-\\d{2})$"); //TODO dovrši sam
		//TODO isto za sve ostale
		Matcher mMeteoIcao = pMeteoIcao.matcher(zahtjev);
		Matcher mMeteoIcaoDatum = pMeteoIcaoDatum.matcher(zahtjev);
		//TODO isto za sve ostale
		
		//TODO provjeri kako mora biti prema opisu zadaće
		String odgovor = "ERROR 10 Neispravna komanda!";
		
		if(mMeteoIcao.matches()) {
			mMeteoIcao.group(1);
			odgovor = izvrsiNaredbuMeteoIcao(zahtjev);
		} else if(mMeteoIcaoDatum.matches()) {
			odgovor = "NAPRAVI SAM!";
		}
		
		return odgovor;
	}

	private String izvrsiNaredbuMeteoIcao(String zahtjev) {
		String[] podaci = zahtjev.split(" ");
		String icao = podaci[1];
		for (AerodromMeteo am : meteoPodaci) {
			//TODO pronađi zadnji
			return "OK "+am.temp+" "+am.vlaga+" "+am.tlak+" "+am.vrijeme+";";
		}
		
		return "ERROR 11 Aerodrom '"+icao+"' ne postoji!";//TODO provjeri opis zadaće
	}
}
