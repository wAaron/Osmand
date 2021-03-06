/**
 * 
 */
package net.osmand.plus.activities;


import java.text.Collator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import net.osmand.data.LatLon;
import net.osmand.osm.PoiCategory;
import net.osmand.osm.PoiType;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.search.SearchActivity;
import net.osmand.plus.activities.search.SearchPOIActivity;
import net.osmand.plus.poi.PoiFiltersHelper;
import net.osmand.plus.poi.PoiLegacyFilter;
import net.osmand.util.Algorithms;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.view.MenuItemCompat;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;

/**
 * 
 */
public class EditPOIFilterActivity extends OsmandListActivity {
	public static final String AMENITY_FILTER = "net.osmand.amenity_filter"; //$NON-NLS-1$
	private PoiLegacyFilter filter;
	private PoiFiltersHelper helper;
	public static final String SEARCH_LAT = SearchActivity.SEARCH_LAT; //$NON-NLS-1$
	public static final String SEARCH_LON = SearchActivity.SEARCH_LON; //$NON-NLS-1$
	private static final int FILTER = 2;
	public static final int EDIT_ACTIVITY_RESULT_OK = 20;
	

	@Override
	public void onCreate(final Bundle icicle) {
		Bundle bundle = this.getIntent().getExtras();
		String filterId = bundle.getString(AMENITY_FILTER);
		helper = ((OsmandApplication) getApplication()).getPoiFilters();
		filter = helper.getFilterById(filterId);
		super.onCreate(icicle);

		setContentView(R.layout.editing_poi_filter);
		getSupportActionBar().setTitle(R.string.filterpoi_activity);
//		getSupportActionBar().setIcon(R.drawable.tab_search_poi_icon);

		if (filter != null) {
			getSupportActionBar().setSubtitle(filter.getName());
			setListAdapter(new AmenityAdapter(  ((OsmandApplication) getApplication()).getPoiTypes().getCategories()));
		} else {
			setListAdapter(new AmenityAdapter(new ArrayList<PoiCategory>()));
		}

	}


	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == FILTER) {
//			filterPOI();
			setResult(EDIT_ACTIVITY_RESULT_OK);
			finish();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
	
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		if(filter == null) {
			return super.onCreateOptionsMenu(menu);
		}
		createMenuItem(menu, FILTER, R.string.filter_current_poiButton, 
				R.drawable.ic_action_done, 
				//R.drawable.a_1_navigation_accept_light, R.drawable.a_1_navigation_accept_dark,
				MenuItemCompat.SHOW_AS_ACTION_WITH_TEXT | MenuItemCompat.SHOW_AS_ACTION_ALWAYS);
		return super.onCreateOptionsMenu(menu);
	}	
	
	private void filterPOI() {
		Bundle extras = getIntent().getExtras();
		boolean searchNearBy = true;
		LatLon lastKnownMapLocation = ((OsmandApplication) getApplication()).getSettings().getLastKnownMapLocation();
		double latitude = lastKnownMapLocation != null ? lastKnownMapLocation.getLatitude() : 0;
		double longitude = lastKnownMapLocation != null ? lastKnownMapLocation.getLongitude() : 0;
		final Intent newIntent = new Intent(EditPOIFilterActivity.this, SearchPOIActivity.class);
		if(extras != null && extras.containsKey(SEARCH_LAT) && extras.containsKey(SEARCH_LON)){
			latitude = extras.getDouble(SEARCH_LAT);
			longitude = extras.getDouble(SEARCH_LON);
			searchNearBy = false;
		}
		final double lat = latitude;
		final double lon = longitude;
		newIntent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
		newIntent.putExtra(SearchPOIActivity.AMENITY_FILTER, filter.getFilterId());
		if (searchNearBy) {
			startActivity(newIntent);
		} else {
			newIntent.putExtra(SearchPOIActivity.SEARCH_LAT, lat);
			newIntent.putExtra(SearchPOIActivity.SEARCH_LON, lon);
			startActivity(newIntent);
		}
	}
	

	

	
	private void showDialog(final PoiCategory poiCategory) {
		ListView lv = EditPOIFilterActivity.this.getListView();
		final int index = lv.getFirstVisiblePosition();
		View v = lv.getChildAt(0);
		final int top = (v == null) ? 0 : v.getTop();
		Builder builder = new AlertDialog.Builder(this);
		ScrollView scroll = new ScrollView(this);
		ListView listView = new ListView(this);
		final LinkedHashMap<String, String> subCategories = new LinkedHashMap<String, String>();
		Set<String> acceptedCategories = filter.getAcceptedSubtypes(poiCategory);
		if (acceptedCategories != null) {
			for(String s : acceptedCategories) {
				subCategories.put(s, Algorithms.capitalizeFirstLetterAndLowercase(s));
			}
		}
		for(PoiType pt :  poiCategory.getPoiTypes()) {
			subCategories.put(pt.getKeyName(), pt.getTranslation());
		}

		final String[] array = subCategories.keySet().toArray(new String[0]);
		final Collator cl = Collator.getInstance();
		cl.setStrength(Collator.SECONDARY);
		Arrays.sort(array, 0, array.length, new Comparator<String>() {

			@Override
			public int compare(String object1, String object2) {
				String v1 = subCategories.get(object1);
				String v2 = subCategories.get(object2);
				return cl.compare(v1, v2);
			}
		});
		final String[] visibleNames = new String[array.length];
		final boolean[] selected = new boolean[array.length];
		
		for (int i = 0; i < array.length; i++) {
			final String subcategory = array[i];
			visibleNames[i] = subCategories.get(subcategory);			
			if (acceptedCategories == null) {
				selected[i] = true;
			} else {
				selected[i] = acceptedCategories.contains(subcategory);
			}
		}

		scroll.addView(listView);
		builder.setView(scroll);
		builder.setNeutralButton(EditPOIFilterActivity.this.getText(R.string.shared_string_close), new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				LinkedHashSet<String> accepted = new LinkedHashSet<String>();
				for (int i = 0; i < selected.length; i++) {
					if(selected[i]){
						accepted.add(array[i]);
					}
				}
				if (subCategories.size() == accepted.size()) {
					filter.selectSubTypesToAccept(poiCategory, null);
				} else if(accepted.size() == 0){
					filter.setTypeToAccept(poiCategory, false);
				} else {
					filter.selectSubTypesToAccept(poiCategory, accepted);
				}
				helper.editPoiFilter(filter);
				ListView lv = EditPOIFilterActivity.this.getListView();
				AmenityAdapter la = (AmenityAdapter) EditPOIFilterActivity.this.getListAdapter();
				la.notifyDataSetChanged();
				lv.setSelectionFromTop(index, top);
			}
		});
	
		builder.setPositiveButton(EditPOIFilterActivity.this.getText(R.string.shared_string_select_all), new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				filter.selectSubTypesToAccept(poiCategory, null);
				helper.editPoiFilter(filter);
				ListView lv = EditPOIFilterActivity.this.getListView();
				AmenityAdapter la = (AmenityAdapter) EditPOIFilterActivity.this.getListAdapter();
				la.notifyDataSetInvalidated();
				lv.setSelectionFromTop(index, top);
			}
		});

		builder.setMultiChoiceItems(visibleNames, selected, new DialogInterface.OnMultiChoiceClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int item, boolean isChecked) {
				selected[item] = isChecked;
			}
		});
		builder.show();

	}
	
	
	@Override
	public AmenityAdapter getListAdapter() {
		return (AmenityAdapter) super.getListAdapter();
	}
	
	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		showDialog(getListAdapter().getItem(position));
	}

	class AmenityAdapter extends ArrayAdapter<PoiCategory> {
		AmenityAdapter(List<PoiCategory> amenityTypes) {
			super(EditPOIFilterActivity.this, R.layout.editing_poi_filter_list, amenityTypes);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			LayoutInflater inflater = getLayoutInflater();
			View row = convertView;
			if (row == null) {
				row = inflater.inflate(R.layout.editing_poi_filter_list, parent, false);
			}
			PoiCategory model = getItem(position);

			CheckBox check = (CheckBox) row.findViewById(R.id.filter_poi_check);
			check.setChecked(filter.isTypeAccepted(model));

			TextView text = (TextView) row.findViewById(R.id.filter_poi_label);
			String textString = model.getTranslation();
			Set<String> subtypes = filter.getAcceptedSubtypes(model);
			if(filter.isTypeAccepted(model)) {
				if(subtypes == null) {
					textString += " (" + getString(R.string.shared_string_all) +")";
				} else {
					textString += " (" + subtypes.size() +")";
				}
			}
			text.setText(textString);
			addRowListener(model, text, check);
			return (row);
		}

		private void addRowListener(final PoiCategory model, final TextView text, final CheckBox check) {
			text.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					showDialog(model);
				}
			});

			check.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					if (check.isChecked()) {
						filter.setTypeToAccept(model, true);
						showDialog(model);
					} else {
						filter.setTypeToAccept(model, false);
						helper.editPoiFilter(filter);
					}
				}
			});
		}

	}

}
