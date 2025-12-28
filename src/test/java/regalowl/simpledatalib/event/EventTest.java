package regalowl.simpledatalib.event;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import regalowl.simpledatalib.SimpleDataLib;
import regalowl.simpledatalib.event.SDLEvent;
import regalowl.simpledatalib.events.LogEvent;
import regalowl.simpledatalib.events.LogLevel;

public class EventTest implements SDLEventListener {
	
	private SimpleDataLib db;
	private String errorMessage;
	
	@BeforeEach
	public void init() {
		db = new SimpleDataLib("test");
		db.initialize();
		db.setDebug(true);
		db.getEventPublisher().registerListener(this);
	}
	
	@Test
	public void testLogEvent() {
		SDLEvent e = db.getEventPublisher().fireEvent(new LogEvent("test", null, LogLevel.INFO));
		assertTrue(e.firedSuccessfully());
	}
	

	@Override
	public void handleSDLEvent(SDLEvent event) {
		if (event instanceof LogEvent) {
			LogEvent levent = (LogEvent)event;
			errorMessage = levent.getMessage();
		}
	}
	
	@AfterEach
	public void after() {
		assertTrue(errorMessage.equals("test"));
	}
}
