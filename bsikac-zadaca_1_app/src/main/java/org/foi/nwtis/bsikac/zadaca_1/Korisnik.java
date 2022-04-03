package org.foi.nwtis.bsikac.zadaca_1;

import lombok.Getter;
import lombok.Setter;
import lombok.NonNull;
import lombok.AllArgsConstructor;

/**
 * 
 * Klasa za korisnika
 *
 */
//@AllArgsConstructor()
public class Korisnik {

	@Getter
	@Setter
	@NonNull
	private String prezime;
	@Getter
	@Setter
	@NonNull
	private String ime;
	@Getter
	@Setter
	@NonNull
	private String korisnickoIme;
	@Getter
	@Setter
	@NonNull
	private String lozinka;

	public Korisnik(@NonNull String prezime, @NonNull String ime, @NonNull String korisnickoIme,
			@NonNull String lozinka) {
		super();
		this.prezime = prezime;
		this.ime = ime;
		this.korisnickoIme = korisnickoIme;
		this.lozinka = lozinka;
	}

	public String getPrezime() {
		return prezime;
	}

	public void setPrezime(String prezime) {
		this.prezime = prezime;
	}

	public String getIme() {
		return ime;
	}

	public void setIme(String ime) {
		this.ime = ime;
	}

	public String getKorisnickoIme() {
		return korisnickoIme;
	}

	public void setKorisnickoIme(String korisnickoIme) {
		this.korisnickoIme = korisnickoIme;
	}

	public String getLozinka() {
		return lozinka;
	}

	public void setLozinka(String lozinka) {
		this.lozinka = lozinka;
	}
	
}
