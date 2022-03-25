package org.foi.nwtis.bsikac.vjezba_03.konfiguracije;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Properties;

public class KonfiguracijaBIN extends KonfiguracijaApstraktna {

	public static final String TIP = "bin";

	public KonfiguracijaBIN(String nazivDatoteke) {
		super(nazivDatoteke);
	}

	@Override
	public void ucitajKonfiguraciju(String nazivDatoteke) throws NeispravnaKonfiguracija {
		String tip = Konfiguracija.dajTipKonfiguracije(nazivDatoteke);

		if (tip == null || tip.compareTo(TIP) != 0) {
			throw new NeispravnaKonfiguracija("Datoteka " + nazivDatoteke + " nije tipa " + TIP);
		}

		File datoteka = new File(nazivDatoteke);
		if (datoteka == null || !datoteka.isFile() || !datoteka.exists() || !datoteka.canRead()) {
			throw new NeispravnaKonfiguracija("Datoteka " + nazivDatoteke + "  ne postoji ili se ne može čitati!");
		}

		try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(datoteka))) {
			Object o = ois.readObject();
			if (o instanceof Properties)
				this.postavke = (Properties) o;
			else
				throw new NeispravnaKonfiguracija("Datoteka " + nazivDatoteke + " ne sadrži objekt tipa Properties!");
		} catch (IOException | ClassNotFoundException e) {
			throw new NeispravnaKonfiguracija(e.getMessage());
		}
	}

	@Override
	public void spremiKonfiguraciju(String nazivDatoteke) throws NeispravnaKonfiguracija {
		String tip = Konfiguracija.dajTipKonfiguracije(nazivDatoteke);

		if (tip == null || tip.compareTo(TIP) != 0) {
			throw new NeispravnaKonfiguracija("Datoteka " + nazivDatoteke + " nije tipa " + TIP);
		}

		File datoteka = new File(nazivDatoteke);
	

		try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(datoteka))) {
			oos.writeObject(this.postavke);
		} catch (IOException e) {
			throw new NeispravnaKonfiguracija(e.getMessage());
		}
	}

}
