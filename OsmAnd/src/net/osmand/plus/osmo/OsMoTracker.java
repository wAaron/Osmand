package net.osmand.plus.osmo;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import net.osmand.Location;
import net.osmand.plus.osmo.OsMoService.OsMoReactor;
import net.osmand.plus.osmo.OsMoService.OsMoSender;
import net.osmand.plus.views.OsmandMapTileView;

import org.json.JSONObject;

public class OsMoTracker implements OsMoSender, OsMoReactor {
	private ConcurrentLinkedQueue<Location> bufferOfLocations = new ConcurrentLinkedQueue<Location>();
	private Map<String, Location> otherLocations = new ConcurrentHashMap<String, Location>();
	private ConcurrentLinkedQueue<String> listenIds = new ConcurrentLinkedQueue<String>();
	private boolean trackerStarted;
	private boolean startSendingLocations;
	private OsmandMapTileView view;

	public OsMoTracker(OsMoService service) {
		service.registerSender(this);
		service.registerReactor(this);
	}
	
	public void enableTracker() {
		startSendingLocations = true;
	}
	
	public void disableTracker() {
		startSendingLocations = false;
	}
	
	public void startTrackingId(String id) {
		listenIds.add("LISTEN|"+id);
		otherLocations.put(id, null);
	}
	
	public void stopTrackingId(String id) {
		listenIds.add("UNLISTEN|"+id);
		otherLocations.remove(id);
	}

	@Override
	public String nextSendCommand(OsMoThread thread) {
		if (trackerStarted != startSendingLocations) {
			if (!trackerStarted) {
				trackerStarted = true;
				return "TRACKER_SESSION_OPEN";
			} else {
				trackerStarted = false;
				return "TRACKER_SESSION_CLOSE";
			}
		}
		if(!listenIds.isEmpty()) {
			return listenIds.poll();
		}
		if(!bufferOfLocations.isEmpty()){
			Location loc = bufferOfLocations.poll();
			StringBuilder cmd = new StringBuilder("T|");
			cmd.append("L").append((float)loc.getLatitude()).append(":").append((float)loc.getLongitude());
			if(loc.hasAccuracy()) {
				cmd.append("H").append((float)loc.getAccuracy());
			}
			if(loc.hasAltitude()) {
				cmd.append("A").append((float)loc.getAltitude());
			}
			if(loc.hasSpeed()) {
				cmd.append("S").append((float)loc.getSpeed());
			}
			if(loc.hasBearing()) {
				cmd.append("C").append((float)loc.getBearing());
			}
			if((System.currentTimeMillis() - loc.getTime()) > 30000) {
				cmd.append("T").append(loc.getTime());
			}
			return cmd.toString(); 
		}
		return null;
	}

	public void sendCoordinate(Location location) {
		bufferOfLocations.add(location);
	}

	public void sendCoordinate(double lat, double lon) {
		Location l = new Location("test");
		l.setLatitude(lat);
		l.setLongitude(lon);
		bufferOfLocations.add(l);
	}
	
	public void setView(OsmandMapTileView view) {
		this.view = view;
	}
	
	public OsmandMapTileView getView() {
		return view;
	}

	@Override
	public boolean acceptCommand(String command, String data, JSONObject obj, OsMoThread thread) {
		if(command.startsWith("LT:")) {
			String tid = command.substring(3);
			float lat = 0;
			float lon = 0;
			float speed = 0;
			int k = 0;
			for (int i = 0; i <= data.length(); i++) {
				boolean separator = i == data.length() || Character.isDigit(data.charAt(i)) || data.charAt(i) == ':'
						|| data.charAt(i) == '.';
				if (separator) {
					char ch = data.charAt(k);
					String vl = data.substring(k + 1, i);
					if (ch == 'L') {
						int l = vl.indexOf(":");
						lat = Float.parseFloat(vl.substring(0, l));
						lon = Float.parseFloat(vl.substring(l + 1));
					} else if (ch == 'S') {
						speed = Float.parseFloat(vl);
					}
					k = i;
				}
			}
			if(lat != 0 || lon != 0) {
				Location loc = new Location("osmo");
				loc.setTime(System.currentTimeMillis());
				loc.setLatitude(lat);
				loc.setLongitude(lon);
				if(speed > 0) {
					loc.setSpeed(speed);
				}
				otherLocations.put(tid, loc);
				OsmandMapTileView v = view;
				if(v != null){
					v.refreshMap();
				}
			}
			return true;
		}
		return false;
	}
	

}