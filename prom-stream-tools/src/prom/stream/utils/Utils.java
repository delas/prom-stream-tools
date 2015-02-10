package prom.stream.utils;

import org.deckfour.xes.in.XMxmlGZIPParser;
import org.deckfour.xes.in.XMxmlParser;
import org.deckfour.xes.in.XParser;
import org.deckfour.xes.in.XesXmlGZIPParser;
import org.deckfour.xes.in.XesXmlParser;

/**
 *
 * @author Andrea Burattin
 */
public class Utils {

	public static XParser getParser(String name) {
		if (name.substring(name.length() - 6).equals("xes.gz")) {
			return new XesXmlGZIPParser();
		} else if (name.substring(name.length() - 3).equals("xes")) {
			return new XesXmlParser();
		} else if (name.substring(name.length() - 4).equals("mxml")) {
			return new XMxmlParser();
		} else if (name.substring(name.length() - 7).equals("mxml.gz")) {
			return new XMxmlGZIPParser();
		}
		return new XesXmlParser();
	}
}
