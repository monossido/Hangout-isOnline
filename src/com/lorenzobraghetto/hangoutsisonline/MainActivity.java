package com.lorenzobraghetto.hangoutsisonline;

import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import android.app.Activity;
import android.app.Dialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.Contacts.Photo;
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
	private List<Friend> friends;
	private List<Uri> friendsContactUri;
	private List<Bitmap> friendsContactPicture;

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
				new connect().execute();

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
			friends = XMPPConnect.XMPPgetFriends(MainActivity.this);
			sortFriends();
			getBadge();
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

				ListAdapter listA = new ListAdapter(MainActivity.this, friends, friendsContactUri, friendsContactPicture);
				listV.setAdapter(listA);
			}

		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	public void sortFriends() {
		Collections.sort(friends, new Comparator<Friend>() {
			@Override
			public int compare(final Friend object1, final Friend object2) {
				return object1.getUser().compareTo(object2.getUser());
			}
		});
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

	private void getBadge() {
		Cursor mCursor;
		int mIdColumn;
		int mLookupKeyColumn;
		Uri mContactUri;

		ContentResolver resolver = getContentResolver();

		friendsContactUri = new ArrayList<Uri>();
		friendsContactPicture = new ArrayList<Bitmap>();

		for (Friend friend : friends) {
			Uri uri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_FILTER_URI, Uri.encode(friend.getUser()));
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
				mCursor = resolver.query(uri, new String[] { Contacts._ID, Contacts.LOOKUP_KEY, Contacts.PHOTO_THUMBNAIL_URI }, null, null, null);
			} else {
				mCursor = resolver.query(uri, new String[] { Contacts._ID, Contacts.LOOKUP_KEY }, null, null, null);
			}

			boolean exist = mCursor.moveToFirst();

			if (exist) {
				mIdColumn = mCursor.getColumnIndex(Contacts._ID);
				mLookupKeyColumn = mCursor.getColumnIndex(Contacts.LOOKUP_KEY);

				mContactUri =
						Contacts.getLookupUri(
								mCursor.getLong(mIdColumn),
								mCursor.getString(mLookupKeyColumn)
								);

				int mThumbnailColumn;
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
					mThumbnailColumn =
							mCursor.getColumnIndex(Contacts.PHOTO_THUMBNAIL_URI);
				} else {
					mThumbnailColumn = mIdColumn;
				}

				friendsContactUri.add(mContactUri);

				String mThumbnailUri = mCursor.getString(mThumbnailColumn);
				if (mThumbnailUri != null) {
					Bitmap mThumbnail =
							loadContactPhotoThumbnail(mThumbnailUri);
					friendsContactPicture.add(mThumbnail);
				} else
					friendsContactPicture.add(null);
			} else {
				friendsContactUri.add(null);
				friendsContactPicture.add(null);
			}

			mCursor.close();
		}
	}

	private Bitmap loadContactPhotoThumbnail(String photoData) {
		AssetFileDescriptor afd = null;
		try {
			Uri thumbUri;
			if (Build.VERSION.SDK_INT
			>= Build.VERSION_CODES.HONEYCOMB) {
				thumbUri = Uri.parse(photoData);
			} else {
				final Uri contactUri = Uri.withAppendedPath(
						Contacts.CONTENT_URI, photoData);
				thumbUri =
						Uri.withAppendedPath(
								contactUri, Photo.CONTENT_DIRECTORY);
			}

			afd = getContentResolver().
					openAssetFileDescriptor(thumbUri, "r");

			FileDescriptor fileDescriptor = afd.getFileDescriptor();
			if (fileDescriptor != null) {
				return BitmapFactory.decodeFileDescriptor(
						fileDescriptor, null, null);
			}
		} catch (FileNotFoundException e) {

		} finally {
			if (afd != null) {
				try {
					afd.close();
				} catch (IOException e) {
				}
			}
		}
		return null;
	}

}
