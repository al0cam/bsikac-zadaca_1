package org.foi.nwtis.bsikac.zadaca_1;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.Charset;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

class ServerGlavniTest {

	ServerGlavni serverGlavni = null;

	@BeforeAll
	static void setUpBeforeClass() throws Exception {
	}

	@AfterAll
	static void tearDownAfterClass() throws Exception {
	}

	@BeforeEach
	void setUp() throws Exception {
		serverGlavni = new ServerGlavni(8003, 10);
	}

	@AfterEach
	void tearDown() throws Exception {
		serverGlavni = null;
	}

	@Test
	@Disabled
	void testMain() {
		fail("Not yet implemented");
	}

	@Test
	@Disabled
	void testUcitajKonfiguraciju() {
		assertNull(serverGlavni.getKonfig());
		boolean odgovor = serverGlavni.ucitajKonfiguraciju("NWTiS_bsikac_4.txt");
		boolean ocekivano = true;
		assertEquals(ocekivano, odgovor);
		assertNotNull(serverGlavni.getKonfig());
		assertNotEquals(0, serverGlavni.getKonfig().dajSvePostavke().size());
		assertNotNull(serverGlavni.getKonfig().dajPostavku("port"));
	}

	@Test
	@Disabled
void testUcitajKorisnike() {
		assertEquals(0, serverGlavni.getKorisnici().size());
		serverGlavni.ucitajKorisnike("korisnici.csv");
		assertNotEquals(0, serverGlavni.getKorisnici().size());

	}

	@Test
	@Disabled
void testObradaZahtjeva() {
		DretvaTest dt = new DretvaTest();
		dt.start();
		
		try {
			Thread.sleep(10);
		} catch (InterruptedException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		String komanda = "METEO LDZA";
		String odgovor = "";
		try (Socket veza = new Socket("localhost", 8003);
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
			odgovor = tekst.toString();
		} catch (SocketException e) {
			System.out.println(e.getMessage());
		} catch (IOException ex) {
			System.out.println(ex.getMessage());
		}
		assertEquals("OK ", odgovor.substring(0,3));
		
		
	}

	public class DretvaTest extends Thread {

		@Override
		public synchronized void start() {
			// TODO Auto-generated method stub
			super.start();
		}

		@Override
		public void run() {
			super.run();
			serverGlavni.obradaZahtjeva();
		}

		@Override
		public void interrupt() {
			// TODO Auto-generated method stub
			super.interrupt();
		}

	}

}
