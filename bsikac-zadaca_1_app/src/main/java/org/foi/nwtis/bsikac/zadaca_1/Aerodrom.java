package org.foi.nwtis.bsikac.zadaca_1;

import lombok.Getter;
import lombok.Setter;
import lombok.NonNull;
import lombok.AllArgsConstructor;

/**
 *
 * Klasa za aerodrom
 */
@AllArgsConstructor()
public class Aerodrom {
    @Getter
    @Setter 
    @NonNull 
    private String icao;
    @Getter
    @Setter 
    @NonNull 
    private String naziv;
    @Getter
    @Setter 
    @NonNull 
    private String gpsGS;
    @Getter
    @Setter 
    @NonNull 
    private String gpsGD;
}
