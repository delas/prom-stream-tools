package prom.stream.models;

import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedList;

import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XTrace;
import org.deckfour.xes.model.impl.XAttributeTimestampImpl;

/**
 * This class represents a stream of a log. This class extends a priority queue, where its comparator is based on the timestamp attribute of
 * the event log. As result, a stream is defined as a set of pairs of event and the referring trace.
 *
 * @author Andrea Burattin
 */
public class LogStream extends LinkedList<XTrace> {

	private static final long serialVersionUID = 4567099357806634230L;
	private long minTimeBetweenActivities = Long.MAX_VALUE;
	private long maxTimeBetweenActivities = Long.MIN_VALUE;
	private long firstExecutionTime = -1;
	private long lastExecutionTime = -1;

	/**
	 * Basic stream constructor. When the stream has been completely filled with the events, {@link #calculateStatistics()} should be
	 * called.
	 */
	public LogStream() {

	}

	/**
	 * This method should be called in order to populate the minimum/maximum time between two activities and for calculating the total
	 * duration time of the log. This method, typically is called when the stream has been completely filled.
	 */
	public void calculateStatistics() {

		Collections.sort(this, new Comparator<XTrace>() {
			@Override
			public int compare(XTrace o1, XTrace o2) {
				XAttributeTimestampImpl date1 = (XAttributeTimestampImpl) o1.get(0).getAttributes().get("time:timestamp");
				XAttributeTimestampImpl date2 = (XAttributeTimestampImpl) o2.get(0).getAttributes().get("time:timestamp");

				return date1.compareTo(date2);
			}
		});

		long previousExecTime = -1;

		for (XTrace t : this) {
			XEvent e = t.get(0);

			long currentActivityTime = ((XAttributeTimestampImpl) e.getAttributes().get("time:timestamp")).getValueMillis();

			if (previousExecTime != -1) {
				long newDuration = currentActivityTime - previousExecTime;
				minTimeBetweenActivities = Math.min(minTimeBetweenActivities, newDuration);
				maxTimeBetweenActivities = Math.max(maxTimeBetweenActivities, newDuration);
			}

			if (firstExecutionTime == -1) {
				firstExecutionTime = currentActivityTime;
			}
			lastExecutionTime = currentActivityTime;
			previousExecTime = currentActivityTime;
		}
	}

	/**
	 * This method returns the {@link Date} of the first execution
	 *
	 * @return the firstExecutionTime
	 */
	public Date getFirstExecutionTime() {
		return new Date(firstExecutionTime);
	}

	/**
	 * This method returns the {@link Date} of the last execution
	 *
	 * @return the lastExecutionTime
	 */
	public Date getLastExecutionTime() {
		return new Date(lastExecutionTime);
	}

	/**
	 * This method returns the maximum time (as milliseconds) between all couples of activities
	 *
	 * @return the maxTimeBetweenActivities
	 */
	public long getMaxTimeBetweenActivities() {
		return maxTimeBetweenActivities;
	}

	/**
	 * This method returns the minimum time (as milliseconds) between all couples of activities
	 *
	 * @return the minTimeBetweenActivities
	 */
	public long getMinTimeBetweenActivities() {
		return minTimeBetweenActivities;
	}

}
