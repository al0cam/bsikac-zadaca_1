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
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.foi.nwtis.bsikac.vjezba_03.konfiguracije.Konfiguracija;
import org.foi.nwtis.bsikac.vjezba_03.konfiguracije.KonfiguracijaApstraktna;
import org.foi.nwtis.bsikac.vjezba_03.konfiguracije.NeispravnaKonfiguracija;

/**
 * Glavna klasa poslužitelja.
 */
public class ServerGlavni {

	/** Broj porta. */
	private int port = 0;

	/** Maksimalni broj cekaca. */
	public int maksCekaca = -1;
	
	/** Maksimalno cekanje na odgovor od servera. */
	public int maksCekanje = 0;
	
	/** Adresa servera udaljenosti. */
	public String serverUdaljenostiAdresa = "";
	
	/** Port servera udaljenosti. */
	public int serverUdaljenostiPort = 0;
	
	/** Adresa servera aerodroma. */
	public String serverAerodromaAdresa = "";
	
	/** Port servera aerodroma. */
	public int serverAerodromaPort = 0;
	
	/** Adresa servera meteo. */
	public String serverMeteoAdresa = "";
	
	/** Port servera meteo. */
	public int serverMeteoPort = 0;
	
	/** Naziv datoteke meduspremnika. */
	public String datotekaMeduspremnik;
	
	/** Meduspremnik. */
	public volatile Meduspremnik meduspremnik;
	
	/** Brojac dretvi. */
	private int brojacDretvi = 0;

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
		if (args.length != 1) {
			System.out.println("ERROR 49 Parametar mora biti naziv konfiguracijske datoteke!");
			return;
		}

		if (!ucitajKonfiguraciju(args[0]))
			return;
		if (!konfiguracijaSadrzi("port"))
			return;
		if (!konfiguracijaSadrzi("maks.cekaca"))
			return;
		if (!konfiguracijaSadrzi("maks.cekanje"))
			return;
		if (!konfiguracijaSadrzi("datoteka.korisnika"))
			return;
		if (!konfiguracijaSadrzi("datoteka.meduspremnika"))
			return;
		if (!konfiguracijaSadrzi("server.aerodroma.port"))
			return;
		if (!konfiguracijaSadrzi("server.aerodroma.adresa"))
			return;
		if (!konfiguracijaSadrzi("server.udaljenosti.port"))
			return;
		if (!konfiguracijaSadrzi("server.udaljenosti.adresa"))
			return;
		if (!konfiguracijaSadrzi("server.meteo.port"))
			return;
		if (!konfiguracijaSadrzi("server.meteo.adresa"))
			return;

		int port = Integer.parseInt(konfig.dajPostavku("port"));
		if (port < 8000 || port > 9999) {
			System.out.println("ERROR 49 Port: " + port + " nije u dozvoljenom rasponu(8000-9999)");
			return;
		}
		if (!portSlobodan(port))
			return;

		int maksCekaca = Integer.parseInt(konfig.dajPostavku("maks.cekaca"));
		String nazivDatotekeKorisnika = konfig.dajPostavku("datoteka.korisnika");
		String nazivDatotekeMeduspremnika= konfig.dajPostavku("datoteka.meduspremnik");
		int maksCekanje = Integer.parseInt(konfig.dajPostavku("maks.cekanje"));
		String serverUdaljenostiAdresa = konfig.dajPostavku("server.udaljenosti.adresa");
		int serverUdaljenostiPort = Integer.parseInt(konfig.dajPostavku("server.udaljenosti.port"));
		String serverAerodromaAdresa = konfig.dajPostavku("server.aerodroma.adresa");
		int serverAerodromaPort = Integer.parseInt(konfig.dajPostavku("server.aerodroma.port"));
		String serverMeteoAdresa = konfig.dajPostavku("server.meteo.adresa");
		int serverMeteoPort = Integer.parseInt(konfig.dajPostavku("server.meteo.port"));

		System.out.println("Server se podiže na portu: " + port);

		ServerGlavni sm = new ServerGlavni(port, maksCekaca, maksCekanje, nazivDatotekeMeduspremnika, serverUdaljenostiAdresa,
				serverUdaljenostiPort, serverAerodromaAdresa, serverAerodromaPort, serverMeteoAdresa, serverMeteoPort);
		sm.ucitajKorisnike(nazivDatotekeKorisnika);
		sm.obradaZahtjeva();
	}

	/**
	 * Konstruktor glavne klase.
	 *
	 * @param port       broj porta.
	 * @param maksCekaca maksimalni broj cekaca.
	 */
	public ServerGlavni(int port, int maksCekaca) {
		this.port = port;
		this.maksCekaca = maksCekaca;
	}

	/**
	 * Prosireni konstruktor glavne klase.
	 *
	 * @param port 						broj porta.
	 * @param maksCekaca 				maksimalni broj cekaca.
	 * @param maksCekanje 				maksimalno dozvoljeno cekanje na odgovor.
	 * @param datotekaMeduspremnik 		naziv datoteke meduspremnika.
	 * @param serverUdaljenostiAdresa 	adresa servera udaljenosti.
	 * @param serverUdaljenostiPort 	port servera udaljenosti.
	 * @param serverAerodromaAdresa 	adresa servera aerodroma.
	 * @param serverAerodromaPort 		port servera aerodroma.
	 * @param serverMeteoAdresa 		adresa servera meteo.
	 * @param serverMeteoPort 			port serverameteo.
	 */
	public ServerGlavni(int port, int maksCekaca, int maksCekanje, String datotekaMeduspremnik, String serverUdaljenostiAdresa,
			int serverUdaljenostiPort, String serverAerodromaAdresa, int serverAerodromaPort, String serverMeteoAdresa,
			int serverMeteoPort) {
		super();
		this.port = port;
		this.maksCekaca = maksCekaca;
		this.maksCekanje = maksCekanje;
		this.serverUdaljenostiAdresa = serverUdaljenostiAdresa;
		this.serverUdaljenostiPort = serverUdaljenostiPort;
		this.serverAerodromaAdresa = serverAerodromaAdresa;
		this.serverAerodromaPort = serverAerodromaPort;
		this.serverMeteoAdresa = serverMeteoAdresa;
		this.serverMeteoPort = serverMeteoPort;
		this.meduspremnik = new Meduspremnik();
		this.datotekaMeduspremnik = datotekaMeduspremnik;
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
//			e.printStackTrace();
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

			FileReader fr = new FileReader(nazivDatotekeKorisnika, Charset.forName("UTF-8"));
			BufferedReader br = new BufferedReader(fr);
			while (true) {
				String linija = br.readLine();
				if (linija == null || linija.isEmpty())
					break;
				String[] p = linija.split(";");
				Korisnik k = new Korisnik(p[0], p[1], p[2], p[3]);
				korisnici.add(k);
			}
			System.out.println("Učitano " + korisnici.size() + " korisnika!");
		} catch (IOException | NumberFormatException e) {
//			e.printStackTrace();
		}
	}

	/**
	 * Obrada zahtjeva.
	 */
	public void obradaZahtjeva() {
		try (ServerSocket ss = new ServerSocket(this.port, this.maksCekaca)) {
			while (true) {
				this.veza = ss.accept();
				DretvaZahtjeva dretvaZahtjeva = new DretvaZahtjeva(veza, konfig, brojacDretvi);

				brojacDretvi += 1;
				dretvaZahtjeva.postaviServerGlavni(this);
				dretvaZahtjeva.start();
			}

		} catch (IOException ex) {
			Logger.getLogger(ServerGlavni.class.getName()).log(Level.SEVERE, null, ex);
		}

	}

	/**
	 * Konfiguracija sadrzi je metoda koja provjerava sadri li konfiguracija odredeni parametar.
	 *
	 * @param kljuc 	je ime parametra.
	 * @return true, 	ako konfiguracija sadrzi parametar.
	 */
	public static boolean konfiguracijaSadrzi(String kljuc) {
		if (konfig.dajPostavku(kljuc) == null || konfig.dajPostavku(kljuc).isEmpty()) {
			System.out.println("ERROR 49 " + kljuc + " nije definiran u konfiguraciji!");
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
	public static boolean portSlobodan(int port) {
		ServerSocket skt;
		try {
			skt = new ServerSocket(port);
			skt.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
//			e.printStackTrace();
			System.out.println("ERROR 49 Port se vec koristi!");
			return false;
		}
		return true;

	}
}
