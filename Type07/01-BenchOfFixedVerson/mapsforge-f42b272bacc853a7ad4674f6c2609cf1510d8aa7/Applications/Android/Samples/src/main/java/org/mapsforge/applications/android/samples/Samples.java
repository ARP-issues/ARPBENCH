/*
 * Copyright 2013-2015 Ludwig M Brinckmann
 * Copyright 2014 devemux86
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.mapsforge.applications.android.samples;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * Start screen for the sample activities.
 */
public class Samples extends Activity {

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.options_menu, menu);
		return true;
	}

	@TargetApi(Build.VERSION_CODES.LOLLIPOP)
	@SuppressWarnings("deprecation")
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		Intent intent;
		switch (item.getItemId()) {
		case R.id.menu_preferences:
			intent = new Intent(this, Settings.class);
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
				intent.setFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT);
			} else {
				intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
			}
			startActivity(intent);
			return true;
		}
		return false;
	}

	@Override
	public void onResume() {
		super.onResume();
		final SharedPreferences preferences = getSharedPreferences("installation", Activity.MODE_PRIVATE);
		final String accepted = "accepted";
		if (!preferences.getBoolean(accepted, false)) {
			final AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle("Warning");
			builder.setCancelable(true);
			builder.setPositiveButton(R.string.startup_dontshowagain,
					new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					preferences.edit().putBoolean(accepted, true)
					.commit();
				}
			});
			builder.setMessage(R.string.startup_message);
			builder.create().show();
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_samples);
		LinearLayout linearLayout = (LinearLayout) findViewById(R.id.samples);
		linearLayout.addView(createButton(RenderTheme4.class, "Map Viewer Rendertheme V4", null));
		linearLayout.addView(createButton(DiagnosticsMapViewer.class, "Diagnostics", null));
		linearLayout.addView(createButton(SimplestMapViewer.class, "Simplest Map Viewer", null));

		linearLayout.addView(createLabel("Raster Maps"));
		linearLayout.addView(createButton(DownloadLayerViewer.class, "Downloading Mapnik", null));
		linearLayout.addView(createButton(DownloadCustomLayerViewer.class, "Custom Tile Source", null));
		linearLayout.addView(createButton(TileStoreLayerViewer.class, "Tile Store (TMS)", null));

		linearLayout.addView(createLabel("Overlays"));
		linearLayout.addView(createButton(OverlayMapViewer.class, "Overlay", null));
		linearLayout.addView(createButton(GridMapViewer.class, "Geographical Grid", null));
		linearLayout.addView(createButton(BubbleOverlay.class, "Bubble Overlay", null));
		linearLayout.addView(createButton(ViewOverlayViewer.class, "View Overlay", null));
		linearLayout.addView(createButton(LocationOverlayMapViewer.class, "Location Overlay", null));
		linearLayout.addView(createButton(ChangingBitmaps.class, "Changing Bitmaps", null));
		linearLayout.addView(createButton(OverlayWithoutBaseMapViewer.class, "Just Overlays, No Map", null));
		linearLayout.addView(createButton(TwoMaps.class, "Two Maps Overlaid", null));

		linearLayout.addView(createLabel("User Interaction"));
		linearLayout.addView(createButton(LongPressAction.class, "Long Press Action", null));
		linearLayout.addView(createButton(MoveAnimation.class, "Move Animation", null));
		linearLayout.addView(createButton(ZoomToBounds.class, "Zoom to Bounds", null));
		linearLayout.addView(createButton(ItemListActivity.class, "Fragment List/View", null));
		linearLayout.addView(createButton(RotateMapViewer.class, "Map Rotation (External)", null));

		linearLayout.addView(createLabel("Dual Map Views"));
		linearLayout.addView(createButton(DualMapViewer.class, "Dual Maps", null));
		linearLayout.addView(createButton(DualMapViewerWithDifferentDisplayModels.class, "Different DisplayModels", null));
		linearLayout.addView(createButton(DualMapViewerWithClampedTileSizes.class, "Clamped Tile Sizes", null));
		linearLayout.addView(createButton(DualMapnikMapViewer.class, "Tied Maps", null));
		linearLayout.addView(createButton(DualOverviewMapViewer.class, "Overview Map", null));
		linearLayout.addView(createButton(MultiMapLowResWorld.class, "Low Res World Background", new OnClickListener() {
			@Override
			public void onClick(final View v) {
				if (!MultiMapLowResWorld.getWorldMapFile(Samples.this).exists()) {
					final AlertDialog.Builder builder = new AlertDialog.Builder(Samples.this);
					builder.setTitle("Warning");
					builder.setCancelable(true);
					builder.setPositiveButton(R.string.downloadnowbutton,
							new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							// TODO show progress and wait for download
							DownloadManager downloadManager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
							DownloadManager.Request downloadRequest = new DownloadManager.Request(
									Uri.parse("http://download.mapsforge.org/maps/world/world.map"));
							downloadRequest.setDescription("Mapsforge low-res world map");
							downloadRequest.setDestinationInExternalFilesDir(Samples.this, SamplesApplication.MAPS, MultiMapLowResWorld.getWorldMapFileName());
							downloadRequest.setVisibleInDownloadsUi(true);
							downloadManager.enqueue(downloadRequest);
						}
					});
					builder.setMessage(R.string.startup_message_multimap);
					builder.create().show();
				} else {
					startActivity(new Intent(Samples.this, MultiMapLowResWorld.class));
				}
			}
		}));
		linearLayout.addView(createButton(SimpleDataStoreMapViewer.class, "Simple User DataStore", null));

		linearLayout.addView(createLabel("Experiments"));
		linearLayout.addView(createButton(MultiLingualMapViewer.class, "Multi-lingual maps", null));
		linearLayout.addView(createButton(RenderThemeChanger.class, "Changing Renderthemes", null));
		linearLayout.addView(createButton(TileSizeChanger.class, "Changing Tile Size", null));
		linearLayout.addView(createButton(StackedLayersMapViewer.class, "Stacked Tiles", null));
		linearLayout.addView(createButton(NoXMLLayout.class, "Without XML Layout", null));
		linearLayout.addView(createButton(BasicMapViewerV3.class, "Old Osmarender (deprecated)", null));
		linearLayout.addView(createButton(LabelLayerMapViewer.class, "Separate LabelLayer (alpha)", null));
		linearLayout.addView(createButton(ClusterMapActivity.class, "Marker clustering (alpha)", null));

	}

	private Button createButton(final Class<?> clazz, String text, final OnClickListener customListener) {
		Button button = new Button(this);
		if (text == null) {
			button.setText(clazz.getSimpleName());
		} else {
			button.setText(text);
		}
		if (customListener == null) {
			button.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View view) {
					startActivity(new Intent(Samples.this, clazz));
				}
			});
		} else {
			button.setOnClickListener(customListener);
		}
		return button;
	}

	private TextView createLabel(String text) {
		TextView textView = new TextView(this);
		textView.setGravity(Gravity.CENTER);
		if (text == null) {
			textView.setText("---------------");
		} else {
			textView.setText(text);
		}
		return textView;
	}

}
