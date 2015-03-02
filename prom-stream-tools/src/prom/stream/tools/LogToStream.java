package prom.stream.tools;

import java.io.File;
import java.io.PrintWriter;

import org.deckfour.xes.in.XParser;
import org.deckfour.xes.info.XLogInfo;
import org.deckfour.xes.info.XLogInfoFactory;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.deckfour.xes.model.impl.XAttributeLiteralImpl;
import org.deckfour.xes.model.impl.XAttributeTimestampImpl;
import org.deckfour.xes.model.impl.XTraceImpl;
import org.processmining.operationalsupport.xml.OSXMLConverter;

import prom.stream.models.LogStream;
import prom.stream.utils.Utils;

/**
 *
 * @author Andrea Burattin
 */
public class LogToStream {

	protected static LogStream logStreamer(XLog log, boolean randomizeStart, boolean tagBeginningEnd) {
		// set the progress bar so to work on the number of events
		XLogInfo info = XLogInfoFactory.createLogInfo(log);

		// get the temporal window in which all activities are executed (this
		// will be useful in case of randomizing the beginning of the events)
		long temporalWindow = info.getLogTimeBoundaries().getEndDate().getTime() - info.getLogTimeBoundaries().getStartDate().getTime();

		// new queue setup
		LogStream eventQueue = new LogStream();

		// iterate through all the events of the log
		for (XTrace t : log) {
			// if we need to add a random time before the trace is executed, this is the moment
			long traceTimeIncrement = 0;
			if (randomizeStart) {
				// get a random time to be summed to all the events of the current trace
				traceTimeIncrement = (long) (Math.random() * temporalWindow);
			}
			// some general trace statistics
			int traceLength = t.size();
			int eventIndex = 0;
			for (XEvent e : t) {
				XTraceImpl t1 = new XTraceImpl(t.getAttributes());
				XAttributeTimestampImpl timestamp = (XAttributeTimestampImpl) e.getAttributes().get("time:timestamp");
				((XAttributeTimestampImpl) e.getAttributes().get("time:timestamp")).setValueMillis(timestamp.getValueMillis()
						+ traceTimeIncrement);

				// event tagging
				if (tagBeginningEnd) {
					if (eventIndex == 0) {
						e.getAttributes().put("stream:lifecycle:trace-transition",
								new XAttributeLiteralImpl("stream:lifecycle:trace-transition", "start"));
					} else if (eventIndex == traceLength - 1) {
						e.getAttributes().put("stream:lifecycle:trace-transition",
								new XAttributeLiteralImpl("stream:lifecycle:trace-transition", "complete"));
					}
				}

				t1.add(e);

				eventQueue.add(t1);
				eventIndex++;
			}
		}

		// calculate statistics over the stream
		eventQueue.calculateStatistics();

		return eventQueue;
	}

	public static void main(String args[]) throws Exception {
		if (args.length != 3) {
			System.err.println("Please use: java -jar LogToStream.jar LOG_FILE DESTINATION TAG_BEGINNING_END");
			System.exit(-1);
		}

		String logFile = args[0];
		String destination = args[1];
		boolean tagBeginningEnd = Boolean.parseBoolean(args[2]);
		System.out.println("Generating stream with");
		System.out.println("  log file: " + logFile);
		System.out.println("  destination: " + destination);
		System.out.println("  tag beginning end: " + tagBeginningEnd);

		XParser parser = Utils.getParser(logFile);
		XLog log = parser.parse(new File(logFile)).get(0);

		LogStream stream = logStreamer(log, true, tagBeginningEnd);

		OSXMLConverter converter = new OSXMLConverter();
		PrintWriter writer = new PrintWriter(destination);
		for (XTrace t : stream) {
			String packet = converter.toXML(t).replace('\n', ' ');
			writer.println(packet);
		}
		if (writer != null) {
			writer.close();
		}
	}
}
