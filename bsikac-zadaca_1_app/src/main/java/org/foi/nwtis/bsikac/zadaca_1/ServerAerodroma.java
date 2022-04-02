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

public class ServerAerodroma {
	private int port = 0;
	private int maksCekaca = -1;
	private int maksCekanje = 0;
	private String serverUdaljenostiAdresa = "";
	private int serverUdaljenostiPort = 0;
	private ConcurrentHashMap<String, String> meduspremnik;

	private Socket veza = null;
	private List<Aerodrom> aeroPodaci = new ArrayList<Aerodrom>();

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
		if (!konfiguracijaSadrzi("datoteka.aerodroma"))
			return;
		if (!konfiguracijaSadrzi("server.udaljenosti.port"))
			return;
		if (!konfiguracijaSadrzi("server.udaljenosti.adresa"))
			return;

		int port = Integer.parseInt(konfig.dajPostavku("port"));
		if (port < 8000 || port > 9999) {
			System.out.println("Port: " + port + " nije u dozvoljenom rasponu(8000-9999)");
			return;
		}
		if (!portSlobodan(port))
			return;

		int maksCekaca = Integer.parseInt(konfig.dajPostavku("maks.cekaca"));
		String nazivDatotekeAeroPodataka = konfig.dajPostavku("datoteka.aerodroma");
		System.out.println(nazivDatotekeAeroPodataka);
		int maksCekanje = Integer.parseInt(konfig.dajPostavku("maks.cekanje"));
		String serverUdaljenostiAdresa = konfig.dajPostavku("server.udaljenosti.adresa");
		int serverUdaljenostiPort = Integer.parseInt(konfig.dajPostavku("server.udaljenosti.port"));

		ServerAerodroma sa = new ServerAerodroma(port, maksCekaca, maksCekanje, serverUdaljenostiAdresa,
				serverUdaljenostiPort);
		if (!sa.ucitajAeroPodatke(nazivDatotekeAeroPodataka))
			return;

		System.out.println("Server se podiže na portu: " + port);
		sa.obradaZahtjeva();
	}

	public ServerAerodroma(int port, int maksCekaca, int maksCekanje, String serverUdaljenostiAdresa,
			int serverUdaljenostiPort) {
		super();
		this.port = port;
		this.maksCekaca = maksCekaca;
		this.maksCekanje = maksCekanje;
		this.serverUdaljenostiAdresa = serverUdaljenostiAdresa;
		this.serverUdaljenostiPort = serverUdaljenostiPort;
		this.meduspremnik = new ConcurrentHashMap<>();
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

	private boolean ucitajAeroPodatke(String nazivDatotekeAeroPodataka) {
		try {

			FileReader fr = new FileReader(nazivDatotekeAeroPodataka, Charset.forName("UTF-8"));
			BufferedReader br = new BufferedReader(fr);
			while (true) {
				String linija = br.readLine();
				if (linija == null || linija.isEmpty())
					break;
				// TODO razmisli o mogućim problemima kod učitavanja
				String[] p = linija.split(";");
				Aerodrom a = new Aerodrom(p[0], p[1], p[2], p[3]);
				aeroPodaci.add(a);
				// System.out.println(linija);
			}
			System.out.println("Učitano " + aeroPodaci.size() + " meteo podataka!");
			br.close();
		} catch (IOException | NumberFormatException e) {
			if (e.getMessage().contains("Permission denied"))
				System.out.println("ERROR 29 Nije omoguceno citanje datoteke u pravima pristupa!");
			else {
				System.out.println("ERROR 29 Datoteka ne postoji!");
				e.printStackTrace();
			}
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

		if (mAero.matches()) {
			odgovor = izvrsiNaredbuAero(zahtjev);
		} else if (mAeroIcao.matches()) {
			odgovor = izvrsiNaredbuAeroIcao(zahtjev);
		} else if (mAeroIcaoBroj.matches()) {
			odgovor = izvrsiNaredbuAeroIcaoBroj(zahtjev);
		}

		return odgovor;
	}

	private String izvrsiNaredbuAero(String zahtjev) {
		String popisRezultata = "";
		if (meduspremnik.containsKey(zahtjev)) {
			popisRezultata = meduspremnik.get(zahtjev);
		} else {
			for (Aerodrom a : aeroPodaci) {
				if (popisRezultata.length() <= 0) {
					popisRezultata = "OK " + a.icao + ";";
				} else {
					popisRezultata = popisRezultata.concat(" " + a.icao + ";");
				}

			}
		}
		if (popisRezultata.length() <= 0)
			popisRezultata = "ERROR 29 Nije pronaden niti jedan aerodrom!";
		else
			meduspremnik.putIfAbsent(zahtjev, popisRezultata);

		return popisRezultata;
	}

	private String izvrsiNaredbuAeroIcao(String zahtjev) {
		String[] podaci = zahtjev.split(" ");
		String icao = podaci[1];
		String popisRezultata = "";
		if (meduspremnik.containsKey(zahtjev))
			popisRezultata = meduspremnik.get(zahtjev);
		else {
			for (Aerodrom a : aeroPodaci) {

				if (a.icao.equals(icao)) {
					if (popisRezultata.length() <= 0) {
						popisRezultata = "OK " + a.icao + " " + a.naziv + " " + a.gpsGS + " " + a.gpsGD + ";";
					} else {
						popisRezultata = popisRezultata
								.concat(" " + a.icao + " " + a.naziv + " " + a.gpsGS + " " + a.gpsGD + ";");
					}
				}
			}
		}
		if (popisRezultata.length() <= 0)
			popisRezultata = "ERROR 21 Aerodrom '" + icao + "' ne postoji!";
		else
			meduspremnik.putIfAbsent(zahtjev, popisRezultata);

		return popisRezultata;
	}

	private String izvrsiNaredbuAeroIcaoBroj(String zahtjev) {
		String[] podaci = zahtjev.split(" ");
		String icao = podaci[1];
		Double brojKm = Double.parseDouble(podaci[2]);
		String popisRezultata = "OK";
		if (meduspremnik.containsKey(zahtjev))
			popisRezultata = meduspremnik.get(zahtjev);
		else {
			for (Aerodrom a : aeroPodaci) {
				if (a.icao.equals(icao)) {
					Socket vezaNaServerUdaljenosti;
					try {
						vezaNaServerUdaljenosti = new Socket(this.serverUdaljenostiAdresa, this.serverUdaljenostiPort);
						for (Aerodrom a2 : aeroPodaci) {
							String komanda = "DISTANCE " + a.icao + " " + a2.icao;
							String odgovor;
							while(true)
							{
//								odgovor = OK
//								odgovor = AIRPORT 
								odgovor = posaljiKomanduSDaljnjimCitanjem(komanda, vezaNaServerUdaljenosti);
								String[] dijeloviOdgovora = odgovor.split(" ");
								if (dijeloviOdgovora[0].contentEquals("OK")){
									if(Double.parseDouble(dijeloviOdgovora[1]) <= brojKm)
									{
										popisRezultata = popisRezultata.concat(" " + a.icao + dijeloviOdgovora[1] + ";");
									}
									break;
								}
								else if (dijeloviOdgovora[0].equals("AIRPORT"))
								{
//									sprema u komandu OK icao icao.ime ... sto se prilikom sljedeceg pokretanja while petlje salje serveru udaljenosti
									komanda = izvrsiNaredbuAeroIcao(odgovor);
								}
							}
						}

						vezaNaServerUdaljenosti.close();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}

				}
			}
		}
		meduspremnik.putIfAbsent(zahtjev, popisRezultata);

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
		private Socket vezaSaUdaljenosti = null;

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
			try (InputStreamReader isr = new InputStreamReader(this.veza.getInputStream(), Charset.forName("UTF-8"));
					OutputStreamWriter osw = new OutputStreamWriter(this.veza.getOutputStream(),
							Charset.forName("UTF-8"));) {

				StringBuilder tekst = new StringBuilder();
				int i = isr.read();
				while (i != -1)
					tekst.append((char) i);

				System.out.println(tekst.toString()); // TODO kasnije obrisati
				this.veza.shutdownInput();

				String odgovor = obradiNaredbu(tekst.toString());
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

	public String posaljiKomanduSDaljnjimCitanjem(String komanda, Socket veza) {
		try (InputStreamReader isr = new InputStreamReader(veza.getInputStream(), Charset.forName("UTF-8"));
				OutputStreamWriter osw = new OutputStreamWriter(veza.getOutputStream(), Charset.forName("UTF-8"));) {

			osw.write(komanda);
			osw.flush();
//			veza.shutdownOutput();
			StringBuilder tekst = new StringBuilder();
			int i = isr.read();
			while (i != -1) {
				i = isr.read();
				tekst.append((char) i);
			}
//			veza.shutdownInput();

			return tekst.toString();
		} catch (SocketException e) {
			System.out.println(e.getMessage());
		} catch (IOException ex) {
			System.out.println(ex.getMessage());
		}
		return null;
	}

}
