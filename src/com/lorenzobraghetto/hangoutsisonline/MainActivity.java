package com.lorenzobraghetto.hangoutsisonline;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.lorenzobraghetto.hangoutsisonline.logic.Friend;
import com.lorenzobraghetto.hangoutsisonline.logic.XMPPConnect;

public class MainActivity extends Activity {

	private ProgressBar progress;
	private ListView listV;
	private Dialog dialog;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		progress = (ProgressBar) findViewById(R.id.progress);
		listV = (ListView) findViewById(R.id.list);

		final SharedPreferences pref = getSharedPreferences("Login", Context.MODE_PRIVATE);

		dialog = new Dialog(MainActivity.this);
		dialog.setContentView(R.layout.dialog);
		dialog.setTitle(R.string.insertdialog);

		final EditText textEmail = (EditText) dialog.findViewById(R.id.email);
		textEmail.setText(pref.getString("user", ""));
		final EditText textPassowrd = (EditText) dialog.findViewById(R.id.password);

		Button dialogButton = (Button) dialog.findViewById(R.id.buttonOk);
		dialogButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Editor edit = pref.edit();
				edit.putString("user", textEmail.getText().toString());
				edit.putString("password", textPassowrd.getText().toString());
				edit.commit();
				dialog.dismiss();
			}
		});

		if (pref.getString("user", "").equals("")) {
			dialog.show();
		} else
			new connect().execute();

	}

	class connect extends AsyncTask<Void, Void, List<Friend>> {

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			progress.setVisibility(View.VISIBLE);
			listV.setVisibility(View.GONE);
		}

		@Override
		protected List<Friend> doInBackground(Void... params) {
			List<Friend> friends = XMPPConnect.XMPPgetFriends(MainActivity.this);
			return friends;
		}

		@Override
		protected void onPostExecute(List<Friend> friends) {
			super.onPostExecute(friends);
			progress.setVisibility(View.GONE);
			listV.setVisibility(View.VISIBLE);

			if (friends == null)
				Toast.makeText(MainActivity.this, R.string.login_error, Toast.LENGTH_LONG).show();
			else {

				ListAdapter listA = new ListAdapter(MainActivity.this, sortFriends(friends));
				listV.setAdapter(listA);
			}
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	public List<Friend> sortFriends(List<Friend> friends) {
		Collections.sort(friends, new Comparator<Friend>() {
			@Override
			public int compare(final Friend object1, final Friend object2) {
				return object1.getUser().compareTo(object2.getUser());
			}
		});

		return friends;
	}

	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		super.onMenuItemSelected(featureId, item);
		switch (item.getItemId()) {
		case R.id.action_refresh:
			new connect().execute();
			break;
		case R.id.action_edit:
			dialog.show();
			break;
		}
		return true;
	}
}
