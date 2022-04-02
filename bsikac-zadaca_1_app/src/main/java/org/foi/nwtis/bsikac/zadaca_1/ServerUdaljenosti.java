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
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.foi.nwtis.bsikac.vjezba_03.konfiguracije.Konfiguracija;
import org.foi.nwtis.bsikac.vjezba_03.konfiguracije.KonfiguracijaApstraktna;
import org.foi.nwtis.bsikac.vjezba_03.konfiguracije.NeispravnaKonfiguracija;

public class ServerUdaljenosti {
	private int port = 0;
	private int maksCekaca = -1;
	private int maksCekanje = 0;
	private String serverAerodromaAdresa = "";
	private int serverAerodromaPort = 0;
	private ConcurrentHashMap<String, String> meduspremnik;
	private ArrayList<Aerodrom> aeroPodaci;

	private Socket veza = null;

//	TODO: remove isoDateFormat
	private static SimpleDateFormat isoDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
	private static Konfiguracija konfig = null;

	public static void main(String[] args) {
		if (args.length != 1) {
			System.out.println("Parametar mora biti naziv konfiguracijske datoteke!");
			return;
		}

		if (!ucitajKonfiguraciju(args[0]))
			return;

		// TODO provjeri jesu li sve postavke koje trebaju biti
		if (!konfiguracijaSadrzi("port"))
			return;
		if (!konfiguracijaSadrzi("maks.cekaca"))
			return;
		if (!konfiguracijaSadrzi("maks.cekanje"))
			return;
		if (!konfiguracijaSadrzi("server.aerodroma.port"))
			return;
		if (!konfiguracijaSadrzi("server.aerodroma.adresa"))
			return;

		int port = Integer.parseInt(konfig.dajPostavku("port"));
		if (port < 8000 || port > 9999) {
			System.out.println("Port: " + port + " nije u dozvoljenom rasponu(8000-9999)");
			return;
		}
		if (!portSlobodan(port))
			return;

		int maksCekaca = Integer.parseInt(konfig.dajPostavku("maks.cekaca"));
		int maksCekanje = Integer.parseInt(konfig.dajPostavku("maks.cekanje"));
		String serverUdaljenostiAdresa = konfig.dajPostavku("server.aerodroma.adresa");
		int serverUdaljenostiPort = Integer.parseInt(konfig.dajPostavku("server.aerodroma.port"));

		ServerUdaljenosti su = new ServerUdaljenosti(port, maksCekaca, maksCekanje, serverUdaljenostiAdresa,
				serverUdaljenostiPort);

		System.out.println("Server se podiže na portu: " + port);
		su.obradaZahtjeva();
	}

	public ServerUdaljenosti(int port, int maksCekaca, int maksCekanje, String serverUdaljenostiAdresa,
			int serverUdaljenostiPort) {
		super();
		this.port = port;
		this.maksCekaca = maksCekaca;
		this.maksCekanje = maksCekanje;
		this.serverAerodromaAdresa = serverAerodromaAdresa;
		this.serverAerodromaPort = serverAerodromaPort;
		this.meduspremnik = new ConcurrentHashMap<>();
		this.aeroPodaci = new ArrayList<Aerodrom>();
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

	public void obradaZahtjeva() {
		try (ServerSocket ss = new ServerSocket(this.port, this.maksCekaca)) {
			while (true) {
				System.out.println("Čekam korisnika!"); // TODO kasnije obrisati
				this.veza = ss.accept();
//				TODO: check function of timeout
				this.veza.setSoTimeout(maksCekanje);
				DretvaObrade dretvaObrade = new DretvaObrade(veza);
				dretvaObrade.start();
			}

		} catch (IOException ex) {
			Logger.getLogger(ServerUdaljenosti.class.getName()).log(Level.SEVERE, null, ex);
		}

	}

	private String obradiNaredbu(String zahtjev) {
		Pattern pDist = Pattern.compile("^DISTANCE ([A-Z]{4}) ([A-Z]{4})$"),
				pDistClear = Pattern.compile("^DISTANCE CLEAR$");

		Matcher mDist = pDist.matcher(zahtjev), 
				mDistClear = pDistClear.matcher(zahtjev);

		String odgovor = "ERROR 30 Format komande nije ispravan";

		if (mDist.matches()) {
			odgovor = izvrsiNaredbuDist(zahtjev);
		} else if (mDistClear.matches()) {
			odgovor = izvrsiNaredbuDistClear(zahtjev);
		}

		return odgovor;
	}

	private String izvrsiNaredbuDist(String zahtjev) {
		String[] podaci = zahtjev.split(" ");
		String icao1 = podaci[1], icao2 = podaci[2];
		String popisRezultata = "";
		Aerodrom aero1 = null, aero2 = null;
		for (Aerodrom a : aeroPodaci) {
			if (a.icao.equals(icao1)) {
				aero1 = a;
			} else if (a.icao.equals(icao2)) {
				aero2 = a;
			}
		}
		if (aero1.equals(null))
		{
			aero1 = dohvatiAerodrom(icao1);
		}
		if (aero2.equals(null))
		{
			aero2 = dohvatiAerodrom(icao2);
		}

			return popisRezultata;
	}

	private Aerodrom dohvatiAerodrom(String icao) {
		String komanda = "AIRPORT "+icao;
		String odgovor = posaljiKomanduAero(serverAerodromaAdresa, serverAerodromaPort, komanda);
		String[] podaci = odgovor.split(" ");
		if(podaci[0].equals("OK"))
		{
//			TODO: splirat string po necem drugom a ne spaceu jer mi space moze bit u nazivu isto RIP
			return new Aerodrom(podaci[1], podaci[2], komanda, odgovor)
		}
		return null;
	}

	private String izvrsiNaredbuDistClear(String zahtjev) {
		String popisRezultata = "OK";
		aeroPodaci.clear();
		if (aeroPodaci.size() > 0)
			popisRezultata = "ERROR 39 nije moguce brisati spremnik!";

		return popisRezultata;
	}

	private static boolean konfiguracijaSadrzi(String kljuc) {
		if (konfig.dajPostavku(kljuc) == null || konfig.dajPostavku(kljuc).isEmpty()) {
			System.out.println("ERROR 29 " + kljuc + " nije definiran u konfiguraciji!");
			return false;
		}
		return true;

	}

	private static boolean portSlobodan(int port) {
		ServerSocket skt;
		try {
			skt = new ServerSocket(port);
			skt.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
//			e.printStackTrace();
			System.out.println("ERROR 29 Port se vec koristi!");
			return false;
		}
		return true;

	}

	public class DretvaObrade extends Thread {
		private Socket veza = null;

		public DretvaObrade(Socket veza) {
			super();
			this.veza = veza;
		}

		@Override
		public synchronized void start() {
			super.start();
		}

		@Override
		public void run() {
			// TODO Auto-generated method stub
			super.run();
			System.out.println("Dretva: " + this.getId());
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
//				Thread.sleep(10000);
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

	}

	static double udaljenostDvijeTockeNaSferi(Aerodrom icao1, Aerodrom icao2) {
// distance between latitudes and longitudes
		double dLat = Math.toRadians(Double.parseDouble(icao2.gpsGS) - Double.parseDouble(icao1.gpsGS));
		double dLon = Math.toRadians(Double.parseDouble(icao2.gpsGD) - Double.parseDouble(icao1.gpsGD));

// convert to radians
		double gs1 = Math.toRadians(Double.parseDouble(icao1.gpsGS));
		double gs2 = Math.toRadians(Double.parseDouble(icao2.gpsGS));

// apply formulae
		double a = Math.pow(Math.sin(dLat / 2), 2) + Math.pow(Math.sin(dLon / 2), 2) * Math.cos(gs1) * Math.cos(gs2);
		double radiusZemlje = 6371;
		double c = 2 * Math.asin(Math.sqrt(a));
		return radiusZemlje * c;
	}

	public String posaljiKomanduAero(String adresa, int port, String komanda) {
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
			System.out.println(e.getMessage());
		} catch (IOException ex) {
			System.out.println(ex.getMessage());
		}
		return null;
	}

}
