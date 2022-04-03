package org.foi.nwtis.bsikac.zadaca_1;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Meduspremnik implements Serializable {
	public List<Komad> spremnik;
	
	public class Komad
	{
		public String zahtjev, odgovor;
		public int pojave = 0;
		public Komad(String zahtjev, String odgovor) {
			super();
			this.zahtjev = zahtjev;
			this.odgovor = odgovor;
			this.pojave += 1;
		}
	}

	public Meduspremnik() {
		super();
		spremnik = new ArrayList<Komad>();
	}
	
	public void sortirajSpremnik()
	{
		for(Komad k: spremnik)
		{
			for(Komad k2: spremnik)
			{
				if(k.pojave < k2.pojave)
				{
					Komad pom = k;
					k = k2;
					k2 = pom;
				}
			}
		}
	}
	
	public void dodaj(String zahtjev, String odgovor)
	{
		spremnik.add(new Komad(zahtjev, odgovor));
	}
	
	public String pronadji(String zahtjev)
	{
		for(Komad k: spremnik)
		{
			if(k.zahtjev.equals(zahtjev))
			{
				k.pojave +=1;
				return k.odgovor;
			}
		}
		return null;
	}

}
