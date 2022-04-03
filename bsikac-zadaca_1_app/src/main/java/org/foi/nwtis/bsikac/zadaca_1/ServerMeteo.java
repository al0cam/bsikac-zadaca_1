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
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.foi.nwtis.bsikac.vjezba_03.konfiguracije.Konfiguracija;
import org.foi.nwtis.bsikac.vjezba_03.konfiguracije.KonfiguracijaApstraktna;
import org.foi.nwtis.bsikac.vjezba_03.konfiguracije.NeispravnaKonfiguracija;


/**
 * Klasa ServerMeteo.
 */
public class ServerMeteo {
	
	/** Broj porta. */
	private int port = 0;
	
	/** Maksimalni broj cekaca. */
	private int maksCekaca = -1;
	
	/** Veza na mrežnu utičnicu. */
	private Socket veza = null;
	
	/** Lista meteo podataka koji se citaju iz datoteke. */
	private List<AerodromMeteo> meteoPodaci = new ArrayList<AerodromMeteo>();
	
	/** ISO format za datum. */
	private static SimpleDateFormat isoDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
	
	/** Konfiguracijski podaci. */
	private static Konfiguracija konfig = null;
	
	/**
	 * Početna metoda.
	 *
	 * @param args 	argumenti
	 */
	public static void main(String[] args) {
		if(args.length != 1) {
			System.out.println("ERROR 19 Parametar mora biti naziv konfiguracijske datoteke!");
			return;
		}
	
		if(!ucitajKonfiguraciju(args[0])) return;
		
		if(!konfiguracijaSadrzi("port")) return;
		if(!konfiguracijaSadrzi("maks.cekaca")) return;
		if(!konfiguracijaSadrzi("datoteka.meteo")) return;
			
		
		int port = Integer.parseInt(konfig.dajPostavku("port"));
		if(port < 8000 || port > 9999)
		{
			System.out.println("ERROR 19 Port: "+port+ " nije u dozvoljenom rasponu(8000-9999)");
			return;
		}
		if(!portSlobodan(port)) return;
		
		int maksCekaca = Integer.parseInt(konfig.dajPostavku("maks.cekaca"));
		String nazivDatotekeMeteoPodataka = konfig.dajPostavku("datoteka.meteo");
		
		ServerMeteo sm = new ServerMeteo(port, maksCekaca);
		if(!sm.ucitajMeteoPodatke(nazivDatotekeMeteoPodataka)) return;
		
		System.out.println("Server se podiže na portu: "+port);
		sm.obradaZahtjeva();
	}

	/**
	 * Konstruktor klase serverMeteo.
	 *
	 * @param port 			broj porta.
	 * @param maksCekaca 	maksimalni broj cekaca.
	 */
	public ServerMeteo(int port, int maksCekaca) {
		this.port = port;
		this.maksCekaca = maksCekaca;
	}
	
	/**
	 * Ucitaj konfiguraciju.
	 *
	 * @param nazivDatoteke 	naziv datoteke
	 * @return true, 			ako je konfiguracija uspjesno ucitana.
	 */
	private static boolean ucitajKonfiguraciju(String nazivDatoteke) {
		try {
			konfig = KonfiguracijaApstraktna.preuzmiKonfiguraciju(nazivDatoteke);
		} catch (NeispravnaKonfiguracija e) {
			System.out.println("ERROR 19 Došlo je do pogreške prilikom učitavanja konfiguracije!");
			e.printStackTrace();
			return false;
		}
		return true;
	}
		
	/**
	 * Ucitaj meteo podatke.
	 *
	 * @param nazivDatotekeMeteoPodataka  	naziv datoteke meteo podataka.
	 * @return true, 						ako su meteo podaci uspjeno ucitani.
	 */
	private boolean ucitajMeteoPodatke(String nazivDatotekeMeteoPodataka) {
		try {
			
			FileReader fr = new FileReader(nazivDatotekeMeteoPodataka,Charset.forName("UTF-8"));
			BufferedReader br = new BufferedReader(fr);
			while(true) {
				String linija = br.readLine();
				if(linija==null || linija.isEmpty()) break;
				String[] p = linija.split(";");
				AerodromMeteo am = new AerodromMeteo(p[0],Double.parseDouble(p[1]),
							Double.parseDouble(p[2]),Double.parseDouble(p[3]),p[4],
							isoDateFormat.parse(p[4]).getTime());
				meteoPodaci.add(am);
			}
			System.out.println("Učitano " + meteoPodaci.size() + " meteo podataka!");
		} catch (IOException | NumberFormatException | ParseException e) {
			if(e.getMessage().contains("Permission denied"))
				System.out.println("ERROR 19 Nije omoguceno citanje datoteke u pravima pristupa!");
			else
				System.out.println("ERROR 19 Datoteka ne postoji!");
			return false;
		}		
		return true;
	}

	/**
	 * Obrada zahtjeva.
	 */
	public void obradaZahtjeva() {

		try (ServerSocket ss = new ServerSocket(this.port, this.maksCekaca)) {			
			while (true) {
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
					this.veza.shutdownInput();
						
					String odgovor = obradiNaredbu(tekst.toString()); 
//					Thread.sleep(10000);
					osw.write(odgovor);
					osw.flush();
					this.veza.shutdownOutput();
				} catch (SocketException e) {
					e.printStackTrace();
				}
			}

		} catch (IOException ex) {
			Logger.getLogger(ServerMeteo.class.getName()).log(Level.SEVERE, null, ex);
		}

	}

	/**
	 * Obradi naredbu je metoda koja se koristi za oredivanje koja ce se metoda dalje koristiti u izvrsavanju programa.
	 *
	 * @param zahtjev 	zahtjev.
	 * @return 			string.
	 */
	private String obradiNaredbu(String zahtjev) {
		Pattern pMeteoIcao = Pattern.compile("^METEO ([A-Z]{4})$");
		Pattern pMeteoIcaoDatum = Pattern.compile("^METEO ([A-Z]{4}) (\\d{4}-\\d{2}-\\d{2})$"); 
		Pattern pTemp = Pattern.compile("^TEMP (\\d,\\d) (\\d,\\d)$");
		Pattern pTempDatum = Pattern.compile("^TEMP (\\d,\\d) (\\d,\\d) (\\d{4}-\\d{2}-\\d{2})$");

		Matcher mMeteoIcao = pMeteoIcao.matcher(zahtjev);
		Matcher mMeteoIcaoDatum = pMeteoIcaoDatum.matcher(zahtjev);
		Matcher mTemp = pTemp.matcher(zahtjev);
		Matcher mTempDatum = pTempDatum.matcher(zahtjev);

		
		String odgovor = "ERROR 10 Format komande nije ispravan";
		
		if(mMeteoIcao.matches()) 
		{
			odgovor = izvrsiNaredbuMeteoIcao(zahtjev);
		} else if(mMeteoIcaoDatum.matches()) {
			odgovor = izvrsiNaredbuMeteoIcaoDatum(zahtjev);
		}
		else if(mTemp.matches())
		{
			odgovor = izvrsiNaredbuTemp(zahtjev);
		}
		else if(mTempDatum.matches())
		{
			odgovor = izvrsiNaredbuTempDatum(zahtjev);
		}
		
		return odgovor;
	}

	/**
	 * Izvrsi naredbu meteo icao.
	 *
	 * @param zahtjev 	zahtjev.
	 * @return  		string.
	 */
	private String izvrsiNaredbuMeteoIcao(String zahtjev) {
		String[] podaci = zahtjev.split(" ");
		String icao = podaci[1];
		String rezultat = "";
		AerodromMeteo pom = null;
		for (AerodromMeteo am : meteoPodaci) {
			if(am.icao.equals(icao))
			{
				if(pom == null)
				{
					pom = am;
				}
				else
				{
					try {
						Date datum1 = isoDateFormat.parse(pom.vrijeme);
						Date datum2 = isoDateFormat.parse(am.vrijeme);
						if(datum1.before(datum2)){
							pom = am;
						}
					} catch (ParseException e) {
						
					}
				}
			}
		}
		if(pom == null)
			rezultat = "ERROR 11 Aerodrom '"+icao+"' ne postoji!";
		else 
			rezultat = "OK "+zamjeniTockuSaZarezom(zaokruzi(pom.temp))+" "+zamjeniTockuSaZarezom(pom.vlaga)+" "+zamjeniTockuSaZarezom(pom.tlak)+" "+pom.vrijeme+";";

		return rezultat;
	}
	
	/**
	 * Izvrsi naredbu meteo icao datum.
	 *
	 * @param zahtjev 	zahtjev.
	 * @return 			string.
	 */
	private String izvrsiNaredbuMeteoIcaoDatum(String zahtjev) {
		String[] podaci = zahtjev.split(" ");
		String icao = podaci[1];
		String datum = podaci[2];
		String popisRezultata = "";
		for (AerodromMeteo am : meteoPodaci) {
			
			if(am.icao.equals(icao) && am.vrijeme.contains(datum)) 
			{
				if(popisRezultata.length() <= 0)
				{
					popisRezultata = "OK "+zamjeniTockuSaZarezom(zaokruzi(am.temp))+" "+zamjeniTockuSaZarezom(am.vlaga)+" "+zamjeniTockuSaZarezom(am.tlak)+" "+am.vrijeme+";";
				}
				else
				{
					popisRezultata = popisRezultata.concat(" "+zamjeniTockuSaZarezom(zaokruzi(am.temp))+" "+zamjeniTockuSaZarezom(am.vlaga)+" "+zamjeniTockuSaZarezom(am.tlak)+" "+am.vrijeme+";");
				}
			}
		}
		if(popisRezultata.length() <= 0)
			popisRezultata = "ERROR 11 Aerodrom '"+icao+"' ne postoji!";
			
		return popisRezultata;
	}
	
	/**
	 * Izvrsi naredbu temp.
	 *
	 * @param zahtjev 	zahtjev.
	 * @return 			string.
	 */
	private String izvrsiNaredbuTemp(String zahtjev) {
		String[] podaci = zahtjev.split(" ");
		double temp1 = Double.parseDouble(podaci[1].replace(',', '.'));
		double temp2 = Double.parseDouble(podaci[2].replace(',', '.'));
		String popisRezultata = "";
		for (AerodromMeteo am : meteoPodaci) {
			if(zaokruzi(am.temp) >= temp1 && zaokruzi(am.temp) <= temp2) 
			{
				if(popisRezultata.length() <= 0)
				{
					popisRezultata = "OK "+am.icao+" "+zamjeniTockuSaZarezom(zaokruzi(am.temp))+" "+zamjeniTockuSaZarezom(am.vlaga)+" "+zamjeniTockuSaZarezom(am.tlak)+" "+am.vrijeme+";";
				}
				else
				{
					popisRezultata = popisRezultata.concat(" "+am.icao+" "+zamjeniTockuSaZarezom(zaokruzi(am.temp))+" "+zamjeniTockuSaZarezom(am.vlaga)+" "+zamjeniTockuSaZarezom(am.tlak)+" "+am.vrijeme+";");
				}
			}
		}
		if(popisRezultata.length() <= 0)
			popisRezultata = "ERROR 11 ne postoje meteo podaci s temepraturom u rasponu od '"+temp1+"' do '"+temp2+"'!";
			
		return popisRezultata;
	}
	
	
	/**
	 * Izvrsi naredbu temp datum.
	 *
	 * @param zahtjev 	zahtjev.
	 * @return 			string.
	 */
	private String izvrsiNaredbuTempDatum(String zahtjev) {
		String[] podaci = zahtjev.split(" ");
		double temp1 = Double.parseDouble(podaci[1].replace(',', '.'));
		double temp2 = Double.parseDouble(podaci[2].replace(',', '.'));
		String datum = podaci[3];
		String popisRezultata = "";
		for (AerodromMeteo am : meteoPodaci) {
			if(am.vrijeme.contains(datum) && zaokruzi(am.temp) >= temp1 && zaokruzi(am.temp) <= temp2 ) 
			{
				if(popisRezultata.length() <= 0)
				{
					popisRezultata = "OK "+am.icao+" "+zamjeniTockuSaZarezom(zaokruzi(am.temp))+" "+zamjeniTockuSaZarezom(am.vlaga)+" "+zamjeniTockuSaZarezom(am.tlak)+" "+am.vrijeme+";";
				}
				else
				{
					popisRezultata = popisRezultata.concat(" "+am.icao+" "+zamjeniTockuSaZarezom(zaokruzi(am.temp))+" "+zamjeniTockuSaZarezom(am.vlaga)+" "+zamjeniTockuSaZarezom(am.tlak)+" "+am.vrijeme+"; ");
				}
			}
		}
		if(popisRezultata.length() <= 0)
			popisRezultata = "ERROR 11 ne postoje meteo podaci s temepraturom u"
					+ " rasponu od '"+temp1+"' do '"+temp2+"' na datum '"+datum+"'!";
			
		return popisRezultata;
	}
	
	
	/**
	 * Konfiguracija sadrzi je metoda koja provjerava sadri li konfiguracija odredeni parametar.
	 *
	 * @param kljuc 	je ime parametra.
	 * @return true, 	ako konfiguracija sadrzi parametar.
	 */
	private static boolean konfiguracijaSadrzi(String kljuc)
	{
		if(konfig.dajPostavku(kljuc)==null || konfig.dajPostavku(kljuc).isEmpty())
		{
			System.out.println("ERROR 19 "+kljuc+" nije definiran u konfiguraciji!");
			return false;
		}
		return true;
		
	}
	
	/**
	 * Port slobodan je metoda koja provjerava je li odredeni port slobodan.
	 *
	 * @param port 		broj porta.
	 * @return true, 	ako je slobodan.
	 */
	private static boolean portSlobodan(int port)
	{
		ServerSocket skt;
		try {
			skt = new ServerSocket(port);
			skt.close();
		} catch (IOException e ) {
			System.out.println("ERROR 19 Port se vec koristi!");
			return false;
		}
		return true;
		
	}
	
	/**
	 * Zaokruzi zaokruzuje broj na jednu decimalu.
	 *
	 * @param broj 	broj.
	 * @return 		double.
	 */
	private double zaokruzi(double broj)
	{
		return Math.round(broj*10.0)/10.0;
	}
	
	/**
	 * Zamjeni tocku sa zarezom.
	 *
	 * @param broj 	broj.
	 * @return 		string.
	 */
	private String zamjeniTockuSaZarezom(double broj)
	{
		return String.valueOf(broj).replace(".", ",");
	}
	
	
}
