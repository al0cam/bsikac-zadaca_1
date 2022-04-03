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


	public static void main(String[] args) {
		if (args.length <= 4) {
			System.out.println("ERROR nedovoljan broj argumenata!");
			return;
		}
		String kulString = String.join(" ", args);
		System.out.println(kulString);
		Pattern pVelikiRegex = Pattern.compile(
"^-k (?<k>[A-Za-z0-9_-]{3,10}) -l (?<l>[A-Za-z0-9\\_\\-\\#\\!]{3,10}) -s (?<s>(\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3})|([A-Za-z.]+)) -p (?<p>[8-9]\\d{3}) -t (?<t>\\d+) (?<velikaGrupa>(?<aerodrom>--aerodrom((?<aerodromNull>$)| (?<aeroIcao>[A-Z]{4})($|( --km (?<brojKm>\\d{1,5})$))))|(?<meteo>--meteo (?<meteoIcao>[A-Z]{4})($|(?<meteoIcaoDatum> --datum (?<meteoIcaoDatumDatum>\\d{2}\\.\\d{2}\\.\\d{4}\\.)$)))|(?<temp>--tempOd (?<tempTemp1>-?\\d\\,\\d) --tempDo (?<tempTemp2>-?\\d\\,\\d)($| --datum (?<tempDatumDatum>\\d{2}\\.\\d{2}\\.\\d{4}\\.)$))|(?<serverGlavni>--spremi$|--vrati$|--isprazni$|--statistika$)|(?<udaljenost>--udaljenost (?<udaljenostMetode>--isprazni$|--aerodromOd (?<udaljenostAerodromOd>[A-Z]{4}) --aerodromDo (?<udaljenostAerodromDo>[A-Z]{4})$)))"
				);
		Matcher mVelikiRegex = pVelikiRegex.matcher(kulString);
		if (mVelikiRegex.matches()) {

			KorisnikGlavni korisnik = new KorisnikGlavni();
			
			String[] velikaGrupa = mVelikiRegex.group("velikaGrupa").split(" ");
			String komanda = dajKomandu(velikaGrupa[0], mVelikiRegex);
			System.out.println(komanda);
			String odgovor = korisnik.posaljiKomandu(mVelikiRegex.group("s"), Integer.parseInt(mVelikiRegex.group("p")),
					mVelikiRegex.group("t"), mVelikiRegex.group("k"), mVelikiRegex.group("l"),
					komanda);
			
			korisnik.ispis(odgovor);

		} else {
			System.out.println("ERROR parametri su krivo uneseni");
			return;
		}

	}

	public KorisnikGlavni() {
		super();
	}

	
	public static String dajKomandu(String grupa, Matcher velikiRegex) {
		switch (grupa) {
		case "--aerodrom": {
			if(velikiRegex.group("aerodromNull") == "")
			{
				return "AIRPORT";
			}else if(velikiRegex.group("brojKm") != null)
			{
				return "AIRPORT "+velikiRegex.group("aeroIcao")+" "+velikiRegex.group("brojKm");
			}else if(velikiRegex.group("aeroIcao") != null)
			{
				return "AIRPORT "+velikiRegex.group("aeroIcao");
			}
		}
		case "--meteo":{
			if(velikiRegex.group("meteoIcao") != null)
			{
				return "METEO "+velikiRegex.group("meteoIcao");
			}else if(velikiRegex.group("meteoIcaoDatumDatum") != null)
			{
				String[] datum = velikiRegex.group("meteoIcaoDatumDatum").split("\\.");
				String datumFormatirano = datum[2]+"-"+datum[1]+"-"+datum[0];
 				return "METEO "+velikiRegex.group("aeroIcao")+" "+datumFormatirano;
			}
		}
		case "--tempOd":{
			if(velikiRegex.group("tempDatumDatum") == null)
			{
				return "TEMP "+velikiRegex.group("tempTemp1")+" "+velikiRegex.group("tempTemp2");
			}else 
			{
				String[] datum = velikiRegex.group("tempDatumDatum").split("\\.");
				String datumFormatirano = datum[2]+"-"+datum[1]+"-"+datum[0];
 				return "TEMP "+velikiRegex.group("tempTemp1")+" "+velikiRegex.group("tempTemp2")+" "+datumFormatirano;
			}
		}
		case "--udaljenost":{
			if(velikiRegex.group("udaljenostAerodromOd") != null && velikiRegex.group("udaljenostAerodromDo") != null)
			{
				return "DISTANCE "+velikiRegex.group("udaljenostAerodromOd")+" "+velikiRegex.group("udaljenostAerodromDo");
			}else 
			{
 				return "DISTANCE CLEAR";
			}
		}
		case "--spremi":{
			break;
		}
		case "--vrati":{
			break;
		}
		case "--isprazni":{
			break;
		}
		case "--statistika":{
			break;
		}
		
		default:
			throw new IllegalArgumentException("Unexpected value: " + grupa);
		}
		return "ERROR format komande nije ispravan";
	}
	
	public String posaljiKomandu(String adresa, int port, String t, String k, String l, String komanda) {
		String auth = "USER "+k+" PASSWORD "+l+" ";
		String novaKomanda = auth+(komanda);
		System.out.println(novaKomanda);
		try (Socket veza = new Socket(adresa, port);
				InputStreamReader isr = new InputStreamReader(veza.getInputStream(), Charset.forName("UTF-8"));
				OutputStreamWriter osw = new OutputStreamWriter(veza.getOutputStream(), Charset.forName("UTF-8"));) {
			
			osw.write(novaKomanda);
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
