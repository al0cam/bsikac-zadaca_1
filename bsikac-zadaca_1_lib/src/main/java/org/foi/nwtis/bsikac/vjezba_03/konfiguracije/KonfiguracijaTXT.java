package org.foi.nwtis.bsikac.vjezba_03.konfiguracije;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class KonfiguracijaTXT extends KonfiguracijaApstraktna {

	public static final String TIP = "txt";

	public KonfiguracijaTXT(String nazivDatoteke) {
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

		try {
			this.postavke.load(new FileInputStream(datoteka));
		} catch (IOException e) {
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
	

		try {
			this.postavke.store(new FileOutputStream(datoteka),"NWTiS 2022.");
		} catch (IOException e) {
			throw new NeispravnaKonfiguracija(e.getMessage());
		}
	}

}
