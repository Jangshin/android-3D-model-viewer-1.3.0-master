package org.andresoviedo.app.model3D.view;

import java.util.Locale;

import org.andresoviedo.app.util.Utils;
import org.andresoviedo.app.util.content.ContentUtils;
import org.andresoviedo.app.util.view.TextActivity;
import org.andresoviedo.dddmodel2.R;

import android.app.ListActivity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

public class MenuActivity extends ListActivity {

	private static final int REQUEST_CODE_OPEN_FILE = 1000;


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_menu);
		setListAdapter(new ArrayAdapter<String>(this, R.layout.activity_menu_item,
				getResources().getStringArray(R.array.menu_items)));
	}
	
	// @Override
	// public boolean onCreateOptionsMenu(Menu menu) {
	// // Inflate the menu; this adds items to the action bar if it is present.
	// getMenuInflater().inflate(R.menu.menu, menu);
	// return true;
	// }

	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		String selectedItem = (String) getListView().getItemAtPosition(position);
		// Toast.makeText(getApplicationContext(), "Click ListItem '" + selectedItem + "'", Toast.LENGTH_LONG).show();
		String selectedAction = selectedItem.replace(' ', '_').toUpperCase(Locale.getDefault());
		try {
				Intent target = Utils.createGetContentIntent();
				Intent intent = Intent.createChooser(target, "选择本地文件");
				try {
					startActivityForResult(intent, REQUEST_CODE_OPEN_FILE);
				} catch (ActivityNotFoundException e) {
					// The reason for the existence of aFileChooser
				}
		} catch (Exception ex) {
			Toast.makeText(getApplicationContext(), ex.getMessage(), Toast.LENGTH_LONG).show();
		}

	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
		case REQUEST_CODE_OPEN_FILE:
			if (resultCode == RESULT_OK) {
				// The URI of the selected file
				final Uri uri = data.getData();
				Log.i("Menu", "Loading '" + uri.toString() + "'");
				if (uri != null) {
					final String path = ContentUtils.getPath(getApplicationContext(), uri);
					if (path != null) {
						launchModelRendererActivity(path);
					} else {
						Toast.makeText(getApplicationContext(), "Problem loading '" + uri.toString() + "'",
								Toast.LENGTH_SHORT).show();
					}
				}
			} else {
				Toast.makeText(getApplicationContext(), "Result when loading file was '" + resultCode + "'",
						Toast.LENGTH_SHORT).show();
			}
		}
	}

	private void launchModelRendererActivity(String filename) {
		Log.i("Menu", "Launching renderer for '" + filename + "'");
		Intent intent = new Intent(getApplicationContext(), ModelActivity.class);
		Bundle b = new Bundle();
		b.putString("uri", filename);
		b.putString("immersiveMode", "true");
		intent.putExtras(b);
		startActivity(intent);
	}
}
