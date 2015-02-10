package prom.stream.tools;

import java.io.File;
import java.io.FileOutputStream;

import org.deckfour.xes.in.XParser;
import org.deckfour.xes.info.XLogInfo;
import org.deckfour.xes.info.XLogInfoFactory;
import org.deckfour.xes.model.XAttributeLiteral;
import org.deckfour.xes.model.XAttributeMap;
import org.deckfour.xes.model.XAttributeTimestamp;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.deckfour.xes.model.impl.XLogImpl;
import org.deckfour.xes.model.impl.XTraceImpl;
import org.deckfour.xes.out.XMxmlSerializer;
import org.deckfour.xes.out.XSerializer;
import org.deckfour.xes.out.XesXmlSerializer;

import prom.stream.utils.Utils;

/**
 * Class to merge two logs in to a sequence
 *
 * @author Andrea Burattin
 */
public class AppendLogs {

	public static void main(String args[]) throws Exception {
		if (args.length != 3) {
			System.err.println("Please use: java -jar AppendLogs.jar LOG_FILE_1 LOG_FILE_2 DESTINATION");
			System.exit(-1);
		}

		String firstLogFile = args[0];
		String secondLogFile = args[1];
		String logDestination = args[2];

		XParser firstParser = Utils.getParser(firstLogFile);
		XParser secondParser = Utils.getParser(secondLogFile);

		XLog firstLog = firstParser.parse(new File(firstLogFile)).get(0);
		XLog secondLog = secondParser.parse(new File(secondLogFile)).get(0);
		XLog merged = merge(firstLog, secondLog, false);

		XSerializer serializer = new XMxmlSerializer();
		if (logDestination.substring(logDestination.length() - 3).equals("xes")) {
			serializer = new XesXmlSerializer();
		}

		serializer.serialize(merged, new FileOutputStream(logDestination));
	}

	/**
	 * The method responsible of the actual merge.
	 *
	 * @param firstLog
	 *            the first log of the sequence
	 * @param secondLog
	 *            the second log of the sequence
	 * @param reuseSameCaseId
	 *            whether the final log has to reuse the same case ids of the provided logs or not
	 * @return the merged log
	 */
	protected static XLog merge(XLog firstLog, XLog secondLog, boolean reuseSameCaseId) {

		XLogInfo firstInfo = XLogInfoFactory.createLogInfo(firstLog);
		XLogInfo secondInfo = XLogInfoFactory.createLogInfo(secondLog);

		long toAddLog2 = firstInfo.getLogTimeBoundaries().getEndDate().getTime() + 1;

		// build the new log, starting from the union of the attribute sets
		XAttributeMap logAttributes = (XAttributeMap) firstLog.getAttributes().clone();
		logAttributes.putAll((XAttributeMap) secondLog.getAttributes().clone());
		XAttributeLiteral logName = ((XAttributeLiteral) logAttributes.get("concept:name"));
		logName.setValue("(" + firstInfo.getLog().getAttributes().get("concept:name") + ", "
				+ secondInfo.getLog().getAttributes().get("concept:name") + ")");
		logAttributes.put("concept:name", logName);
		XLog finalLog = new XLogImpl(logAttributes);

		// in order to have a unique case id, we have to recompute it from
		// scratch
		int caseId = 0;

		// add the elements of the first log
		for (XTrace t : firstLog) {
			XTrace newT = new XTraceImpl((XAttributeMap) t.getAttributes().clone());
			for (XEvent e : t) {
				e = (XEvent) e.clone();
				XAttributeMap m = e.getAttributes();
				XAttributeTimestamp time = (XAttributeTimestamp) m.get("time:timestamp").clone();
				time.setValueMillis(time.getValueMillis());
				m.put("time:timestamp", time);
				e.setAttributes(m);
				newT.add(e);
			}
			XAttributeMap m = newT.getAttributes();
			XAttributeLiteral c = (XAttributeLiteral) m.get("concept:name");
			c.setValue("case_id_" + (caseId++));
			m.put("concept:name", c);
			finalLog.add(newT);
		}

		// now we have to check if the same case id is supposed to be reused
		if (reuseSameCaseId) {
			caseId = 0;
		}

		// add the elements of the second log
		for (XTrace t : secondLog) {

			// get the current process instance (it depends if we have to
			// recycle the case id or not)
			XTrace newT;
			if (reuseSameCaseId) {
				newT = finalLog.get(caseId);
			} else {
				newT = new XTraceImpl((XAttributeMap) t.getAttributes().clone());
				XAttributeMap m = newT.getAttributes();
				XAttributeLiteral c = (XAttributeLiteral) m.get("concept:name");
				c.setValue("case_id_" + (caseId));
				m.put("concept:name", c);
			}

			// add all the new events to the process instance
			for (XEvent e : t) {
				e = (XEvent) e.clone();
				XAttributeMap m = e.getAttributes();
				XAttributeTimestamp time = (XAttributeTimestamp) m.get("time:timestamp").clone();
				time.setValueMillis(time.getValueMillis() + toAddLog2);
				m.put("time:timestamp", time);
				e.setAttributes(m);
				newT.add(e);
			}

			if (!reuseSameCaseId) {
				finalLog.add(newT);
			}

			caseId++;
		}
		return finalLog;
	}

}
