package org.foi.nwtis.bsikac.zadaca_1;

import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

/**
 * Klasa za meteo zapis aerodroma
 */
//@AllArgsConstructor()
public class AerodromMeteo implements Serializable {
	private static final long serialVersionUID = 1L;
	@Getter
	@Setter
	@NonNull
	String icao;
	@Getter
	@Setter
	double temp;
	@Getter
	@Setter
	double tlak;
	@Getter
	@Setter
	double vlaga;
	@Getter
	@Setter
	@NonNull
	String vrijeme;
	@Getter
	@Setter
	long time;

	public AerodromMeteo(@NonNull String icao, double temp, double tlak, double vlaga, @NonNull String vrijeme,
			long time) {
		super();
		this.icao = icao;
		this.temp = temp;
		this.tlak = tlak;
		this.vlaga = vlaga;
		this.vrijeme = vrijeme;
		this.time = time;
	}
	
}
