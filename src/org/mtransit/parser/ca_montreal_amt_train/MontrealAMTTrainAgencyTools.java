package org.mtransit.parser.ca_montreal_amt_train;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.mtransit.parser.DefaultAgencyTools;
import org.mtransit.parser.Utils;
import org.mtransit.parser.gtfs.data.GCalendar;
import org.mtransit.parser.gtfs.data.GCalendarDate;
import org.mtransit.parser.gtfs.data.GRoute;
import org.mtransit.parser.gtfs.data.GSpec;
import org.mtransit.parser.gtfs.data.GStop;
import org.mtransit.parser.gtfs.data.GTrip;
import org.mtransit.parser.mt.data.MAgency;
import org.mtransit.parser.mt.data.MRoute;
import org.mtransit.parser.CleanUtils;
import org.mtransit.parser.mt.data.MTrip;

// http://www.amt.qc.ca/developers/
// http://www.amt.qc.ca/xdata/trains/google_transit.zip
public class MontrealAMTTrainAgencyTools extends DefaultAgencyTools {

	public static void main(String[] args) {
		if (args == null || args.length == 0) {
			args = new String[3];
			args[0] = "input/gtfs.zip";
			args[1] = "../../mtransitapps/ca-montreal-amt-train-android/res/raw/";
			args[2] = ""; // files-prefix
		}
		new MontrealAMTTrainAgencyTools().start(args);
	}

	private HashSet<String> serviceIds;

	@Override
	public void start(String[] args) {
		System.out.printf("\nGenerating AMT train data...");
		long start = System.currentTimeMillis();
		this.serviceIds = extractUsefulServiceIds(args, this);
		super.start(args);
		System.out.printf("\nGenerating AMT train data... DONE in %s.\n", Utils.getPrettyDuration(System.currentTimeMillis() - start));
	}

	@Override
	public boolean excludeCalendar(GCalendar gCalendar) {
		if (this.serviceIds != null) {
			return excludeUselessCalendar(gCalendar, this.serviceIds);
		}
		return super.excludeCalendar(gCalendar);
	}

	@Override
	public boolean excludeCalendarDate(GCalendarDate gCalendarDates) {
		if (this.serviceIds != null) {
			return excludeUselessCalendarDate(gCalendarDates, this.serviceIds);
		}
		return super.excludeCalendarDate(gCalendarDates);
	}

	@Override
	public boolean excludeTrip(GTrip gTrip) {
		if (this.serviceIds != null) {
			return excludeUselessTrip(gTrip, this.serviceIds);
		}
		return super.excludeTrip(gTrip);
	}

	@Override
	public Integer getAgencyRouteType() {
		return MAgency.ROUTE_TYPE_TRAIN;
	}

	private static final String AGENCY_COLOR = "20558A";

	@Override
	public String getAgencyColor() {
		return AGENCY_COLOR;
	}

	@Override
	public String getRouteLongName(GRoute gRoute) {
		return cleanRouteLongName(gRoute.getRouteLongName());
	}

	private String cleanRouteLongName(String result) {
		result = CleanUtils.SAINT.matcher(result).replaceAll(CleanUtils.SAINT_REPLACEMENT);
		return CleanUtils.cleanLabel(result);
	}

	@Override
	public void setTripHeadsign(MRoute mRoute, MTrip mTrip, GTrip gTrip, GSpec gtfs) {
		mTrip.setHeadsignString(cleanTripHeadsign(gTrip.getTripHeadsign()), gTrip.getDirectionId());
	}

	private static final Pattern DIRECTION = Pattern.compile("(direction )", Pattern.CASE_INSENSITIVE);
	@Override
	public String cleanTripHeadsign(String tripHeading) {
		tripHeading = DIRECTION.matcher(tripHeading).replaceAll(StringUtils.EMPTY);
		return CleanUtils.cleanLabelFR(tripHeading);
	}

	private static List<String> VH = Arrays.asList(new String[] { "Beaconsfield", "Hudson", "Vaudreuil" });
	private static String VH_HV = "Vaudreuil / Hudson / Beaconsfield";
	private static List<String> DM = Arrays.asList(new String[] { "Roxboro-Pierrefonds", "Deux-Montagnes" });
	private static String DM_HV = "Deux-Montagnes / Roxboro-Pierrefonds";

	@Override
	public boolean mergeHeadsign(MTrip mTrip, MTrip mTripToMerge) {
		if (VH.contains(mTrip.getHeadsignValue()) && VH.contains(mTripToMerge.getHeadsignValue())) {
			mTrip.setHeadsignString(VH_HV, mTrip.getHeadsignId());
			return true;
		}
		if (DM.contains(mTrip.getHeadsignValue()) && DM.contains(mTripToMerge.getHeadsignValue())) {
			mTrip.setHeadsignString(DM_HV, mTrip.getHeadsignId());
			return true;
		}
		return super.mergeHeadsign(mTrip, mTripToMerge);
	}

	private static final Pattern GARE = Pattern.compile("(gare )", Pattern.CASE_INSENSITIVE);

	@Override
	public String cleanStopName(String gStopName) {
		gStopName = GARE.matcher(gStopName).replaceAll(StringUtils.EMPTY);
		gStopName = CleanUtils.CLEAN_EN_DASHES.matcher(gStopName).replaceAll(CleanUtils.CLEAN_EN_DASHES_REPLACEMENT);
		return CleanUtils.cleanLabelFR(gStopName);
	}

	@Override
	public String getStopCode(GStop gStop) {
		return gStop.getStopId(); // using stop ID as stop code (useful to match with GTFS real-time)
	}

	private static final String ZERO = "0";

	private static final String A = "A";
	private static final String B = "B";
	private static final String C = "C";
	private static final String D = "D";

	@Override
	public int getStopId(GStop gStop) {
		String stopCode = gStop.getStopCode();
		if (stopCode == null || stopCode.equals(ZERO)) {
			System.out.printf("\nUnexpected stop ID %s!\n", gStop);
			System.exit(-1);
		}
		int stopId = Integer.valueOf(stopCode); // using stop code as stop ID
		if (gStop.getStopId().endsWith(A)) {
			return 1000000 + stopId;
		} else if (gStop.getStopId().endsWith(B)) {
			return 2000000 + stopId;
		} else if (gStop.getStopId().endsWith(C)) {
			return 3000000 + stopId;
		} else if (gStop.getStopId().endsWith(D)) {
			return 4000000 + stopId;
		}
		System.out.printf("\nUnexpected stop ID %s!\n", gStop);
		System.exit(-1);
		return -1;
	}
}
