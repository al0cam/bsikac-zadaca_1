package org.foi.nwtis.bsikac.zadaca_1;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.Charset;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class KorisnikGlavni {

	private String lozinka;
	private String korisnik;
	private String adresa;
	private int port;
	private int cekanje;

	public static void main(String[] args) {
		if (args.length <= 4) {
			System.out.println("ERROR!");
			return;
		}
		String kulString = String.join(" ", args);
		System.out.println(kulString);
		Pattern pVelikiRegex = Pattern.compile(
				"^-k (?<k>[A-Za-z0-9_-]{3,10}) -l (?<l>[A-Za-z0-9\\_\\-\\#\\!]{3,10}) -s (?<s>(\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3})|([A-Za-z.]+)) -p (?<p>[8-9]\\d{3}) -t (?<t>\\d+) (?<velikaGrupa>(?<aerodrom>--aerodrom((?<aerodromNull>$)| (?<aeroIcao>[A-Z]{4}$)| (?<aeroIcaoBrojKm>[A-Z]{4} \\d{1,5}$)))|(?<meteo>--meteo (?<meteoIcao>[A-Z]{4})$|(?<meteoIcaoDatum>(?<meteoIcaoDatumIcao>[A-Z]{4}) --datum (?<meteoIcaoDatumDatum>\\d{2}\\.\\d{2}\\.\\d{4})$))|(?<temp>--tempOd (?<tempTemp1>-?\\d\\,\\d) --tempDo (?<tempTemp2>-?\\d\\,\\d)($| --datum (?<tempDatumDatum>\\d{2}\\.\\d{2}\\.\\d{4})$))|(?<serverGlavni>--spremi$|--vrati$|--isprazni$|--statistika$)|(?<udaljenost>--udaljenost (?<udaljenostMetode>--isprazni$|--aerodromOd (?<udaljenostAerodromOd>[A-Z]{4}) --aerodromDo (?<udaljenostAerodromDo>[A-Z]{4})$)))"
				);
		Matcher mVelikiRegex = pVelikiRegex.matcher(kulString);
		if (mVelikiRegex.matches()) {

			KorisnikGlavni korisnik = new KorisnikGlavni(mVelikiRegex.group("l"), mVelikiRegex.group("k"),
					mVelikiRegex.group("s"), Integer.parseInt(mVelikiRegex.group("p")),
					Integer.parseInt(mVelikiRegex.group("t")));
			
			String[] velikaGrupa = mVelikiRegex.group("velikaGrupa").split(" ");
			String odgovor = korisnik.posaljiKomandu("localhost", 8001, "AIRPORT LDZA");
			switch (velikaGrupa[0]) {
			case "--aerodrom": {
//				TODO: FUNKCIJA ZA AERODROM KOMANDE
				System.out.println("AERODROM KOMANDE");
				break;
			}
			case "--meteo":{
//				TODO: FUNKCIJA ZA METEO KOMANDE
				break;
			}
			case "--tempOd":{
//				TODO: FUNKCIJA ZA temp KOMANDE
				break;
			}
			case "--udaljenost":{
//				TODO: FUNKCIJA ZA udaljenost KOMANDE
				break;
			}
			case "--spremi":{
//				TODO: FUNKCIJA ZA METEO KOMANDE
				break;
			}
			case "--vrati":{
//				TODO: FUNKCIJA ZA METEO KOMANDE
				break;
			}
			case "--isprazni":{
//				TODO: FUNKCIJA ZA METEO KOMANDE
				break;
			}
			case "--statistika":{
//				TODO: FUNKCIJA ZA METEO KOMANDE
				break;
			}
			
			default:
				throw new IllegalArgumentException("Unexpected value: " + velikaGrupa[0]);
			}
			korisnik.ispis(odgovor);

		} else {
			System.out.println("ERROR parametri su krivo uneseni");
			return;
		}

	}

	public KorisnikGlavni(String lozinka, String korisnik, String adresa, int port, int cekanje) {
		super();
		this.lozinka = lozinka;
		this.korisnik = korisnik;
		this.adresa = adresa;
		this.port = port;
		this.cekanje = cekanje;
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
