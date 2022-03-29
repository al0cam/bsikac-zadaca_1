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

// TODO: Auto-generated Javadoc
/**
 * Glavna klasa poslužitelja.
 */
public class ServerGlavni {
	
	/** Broj porta. */
	private int port = 0;
	
	/** Maksimalni broj cekaca. */
	private int maksCekaca = -1;
	
	/** Veza na mrežnu utičnicu. */
	private Socket veza = null;
	
	/** Kolekcija korisnika. */
	private List<Korisnik> korisnici = new ArrayList<Korisnik>();
	
	/** ISO format za datum. */
	private static SimpleDateFormat isoDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
	
	/** Konfiguracijski podaci. */
	private static Konfiguracija konfig = null;
	
	
	/**
	 * Vraća kolekciju korisnika.
	 *
	 * @return the korisnici
	 */
	public List<Korisnik> getKorisnici() {
		return korisnici;
	}

	/**
	 * Vraća konfiguraciju konfig.
	 *
	 * @return konfig
	 */
	public static Konfiguracija getKonfig() {
		return konfig;
	}

	/**
	 * Početna metoda.
	 *
	 * @param args argumenti.
	 */
	public static void main(String[] args) {
		if(args.length != 1) {
			System.out.println("Parametar mora biti naziv konfiguracijske datoteke!");
			return;
		}
	
		if(!ucitajKonfiguraciju(args[0])) return;
		
		//TODO provjeri jesu li sve postavke koje trebaju biti
		
		int port = Integer.parseInt(konfig.dajPostavku("port"));
		int maksCekaca = Integer.parseInt(konfig.dajPostavku("maks.cekaca"));
		String nazivDatotekeKorisnika = konfig.dajPostavku("datoteka.korisnika");
		System.out.println("Server se podiže na portu: "+port);
		
		ServerGlavni sm = new ServerGlavni(port,maksCekaca);
		sm.ucitajKorisnike(nazivDatotekeKorisnika);
		sm.obradaZahtjeva();
	}

	/**
	 * Konstruktor glavne klase.
	 *
	 * @param port broj porta
	 * @param maksCekaca maksimalni broj cekaca.
	 */
	public ServerGlavni(int port, int maksCekaca) {
		this.port = port;
		this.maksCekaca = maksCekaca;
	}
	
	/**
	 * Ucitaj konfiguraciju.
	 *
	 * @param nazivDatoteke naziv datoteke
	 * @return true, ako je uspješno
	 */
	public static boolean ucitajKonfiguraciju(String nazivDatoteke) {
		try {
			konfig = KonfiguracijaApstraktna.preuzmiKonfiguraciju(nazivDatoteke);
		} catch (NeispravnaKonfiguracija e) {
			// TODO Javi nešto pametno
			e.printStackTrace();
			return false;
		}
		return true;
	}
		
	/**
	 * Ucitaj korisnike.
	 *
	 * @param nazivDatotekeKorisnika naziv datoteke korisnika.
	 */
	public void ucitajKorisnike(String nazivDatotekeKorisnika) {
		try {
			
			FileReader fr = new FileReader(nazivDatotekeKorisnika,Charset.forName("UTF-8"));
			BufferedReader br = new BufferedReader(fr);
			while(true) {
				String linija = br.readLine();
				if(linija==null || linija.isEmpty()) break;
				//TODO razmisli o mogućim problemima kod učitavanja
				String[] p = linija.split(";");
				Korisnik k = new Korisnik(p[0],p[1],p[2],p[3]);
				korisnici.add(k);
				//System.out.println(linija);
			}
			System.out.println("Učitano "+korisnici.size()+" korisnika!");
		} catch (IOException | NumberFormatException e) {
			// TODO Napiši nešto pametno
			e.printStackTrace();
		}		
	}

	/**
	 * Obrada zahtjeva.
	 */
	public void obradaZahtjeva() {

		try (ServerSocket ss = new ServerSocket(this.port, this.maksCekaca)) {			
			while (true) {
				System.out.println("Čekam korisnika!"); //TODO kasnije obrisati
				this.veza = ss.accept();
				DretvaZahtjeva dretvaZahtjeva = new DretvaZahtjeva(veza, konfig);
				dretvaZahtjeva.start();
			}

		} catch (IOException ex) {
			Logger.getLogger(ServerGlavni.class.getName()).log(Level.SEVERE, null, ex);
		}

	}
	
}
