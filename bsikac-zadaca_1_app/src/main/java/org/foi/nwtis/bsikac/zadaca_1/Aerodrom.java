package org.foi.nwtis.bsikac.zadaca_1;

import lombok.Getter;
import lombok.Setter;
import lombok.NonNull;
import lombok.AllArgsConstructor;

/**
 *
 * Klasa za aerodrom
 */
//@AllArgsConstructor()
public class Aerodrom {
    
	@Getter
    @Setter 
    @NonNull 
    String icao;
    @Getter
    @Setter 
    @NonNull 
    String naziv;
    @Getter
    @Setter 
    @NonNull 
    String gpsGS;
    @Getter
    @Setter 
    @NonNull 
    String gpsGD;
	public Aerodrom(@NonNull String icao, @NonNull String naziv, @NonNull String gpsGS, @NonNull String gpsGD) {
		super();
		this.icao = icao;
		this.naziv = naziv;
		this.gpsGS = gpsGS;
		this.gpsGD = gpsGD;
	}
	
	
    
}	
