package com.lorenzobraghetto.hangoutsisonline;

import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import android.accounts.AccountManager;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
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
import android.text.SpannableString;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.view.View;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.widget.SearchView;
import com.actionbarsherlock.widget.SearchView.OnQueryTextListener;
import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.auth.UserRecoverableAuthException;
import com.google.android.gms.common.AccountPicker;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.lorenzobraghetto.hangoutsisonline.logic.Friend;
import com.lorenzobraghetto.hangoutsisonline.logic.XMPPConnect;

public class MainActivity extends SherlockActivity implements OnQueryTextListener {

	private ProgressBar progress;
	private ListView listV;
	private List<Friend> friends;
	private List<Uri> friendsContactUri;
	private List<Bitmap> friendsContactPicture;
	public static String mScope = "https://www.googleapis.com/auth/googletalk";
	private boolean rebuilded;
	private SharedPreferences pref;
	public ListAdapter listA;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		rebuilded = false;

		progress = (ProgressBar) findViewById(R.id.progress);
		listV = (ListView) findViewById(R.id.list);

		pref = getSharedPreferences("Login", Context.MODE_PRIVATE);

		if (pref.getString("token", "").equals("")) {
			int result = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
			if (result == ConnectionResult.SUCCESS) {
				Intent intent = AccountPicker.newChooseAccountIntent(null, null, new String[] { "com.google" },
						false, null, null, null, null);
				startActivityForResult(intent, 0);
			} else {
				GooglePlayServicesUtil.getErrorDialog(result, this, 1, new OnCancelListener() {

					@Override
					public void onCancel(DialogInterface dialog) {
						finish();
					}
				}).show();
			}
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
			if (friends != null) {
				sortFriendsByStatus();
				getBadge();
			}
			return friends;
		}

		@Override
		protected void onPostExecute(List<Friend> friends) {
			super.onPostExecute(friends);
			progress.setVisibility(View.GONE);
			listV.setVisibility(View.VISIBLE);

			if (friends == null) {
				if (rebuilded)
					Toast.makeText(MainActivity.this, R.string.login_error, Toast.LENGTH_LONG).show();
				else {
					rebuilded = true;
					new AuthTask().execute(new String[] { pref.getString("user", "") });
				}
			} else {
				listA = new ListAdapter(MainActivity.this, friends, friendsContactUri, friendsContactPicture);
				listV.setAdapter(listA);
			}

		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getSupportMenuInflater().inflate(R.menu.main, menu);
		SearchView searchView = (SearchView) menu.findItem(R.id.action_search).getActionView();
		searchView.setOnQueryTextListener(this);
		return true;
	}

	public void sortFriendsAlphabetically(List<Friend> friends) {
		Collections.sort(friends, new Comparator<Friend>() {
			@Override
			public int compare(final Friend object1, final Friend object2) {
				return object1.getUser().compareTo(object2.getUser());
			}
		});
	}

	public void sortFriendsByStatus() {
		List<Friend> friendsAvaiable = new ArrayList<Friend>();
		List<Friend> friendsAway = new ArrayList<Friend>();
		List<Friend> friendsOffline = new ArrayList<Friend>();
		for (Friend f : friends) {
			if (f.getPresence().isAvailable() && !f.getPresence().isAway())
				friendsAvaiable.add(f);
			else if (f.getPresence().isAway())
				friendsAway.add(f);
			else
				friendsOffline.add(f);
		}
		sortFriendsAlphabetically(friendsAvaiable);
		sortFriendsAlphabetically(friendsAway);
		sortFriendsAlphabetically(friendsOffline);

		friends.clear();

		for (Friend f : friendsAvaiable)
			friends.add(f);
		for (Friend f : friendsAway)
			friends.add(f);
		for (Friend f : friendsOffline)
			friends.add(f);
	}

	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		super.onMenuItemSelected(featureId, item);
		TextView message = new TextView(this);
		switch (item.getItemId()) {
		case R.id.action_refresh:
			new connect().execute();
			break;
		case R.id.action_edit:
			Intent intent = AccountPicker.newChooseAccountIntent(null, null, new String[] { "com.google" },
					false, null, null, null, null);
			startActivityForResult(intent, 0);
			break;
		case R.id.action_info:
			AlertDialog.Builder builder = new AlertDialog.Builder(this);

			final SpannableString s =
					new SpannableString(getText(R.string.info));
			Linkify.addLinks(s, Linkify.WEB_URLS | Linkify.EMAIL_ADDRESSES);

			message.setText(s);
			message.setMovementMethod(LinkMovementMethod.getInstance());

			builder.setView(message)
					.setTitle(R.string.action_info);

			builder.create().show();
			break;
		case R.id.action_sort_by_status:
			if (item.isChecked()) {
				item.setChecked(false);
				sortFriendsAlphabetically(friends);
			} else {
				item.setChecked(true);
				sortFriendsByStatus();
			}
			listA.notifyDataSetChanged();
			break;
		}
		return true;
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		if (requestCode == 0 && resultCode == RESULT_OK) {
			String accountName = data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
			new AuthTask().execute(new String[] { accountName });
		}
	}

	private class AuthTask extends AsyncTask<String, Void, Void> {

		private String name;
		private String token;

		@Override
		protected Void doInBackground(String... params) {
			name = params[0];
			try {
				token = GoogleAuthUtil.getToken(MainActivity.this, name, "oauth2:" + mScope);
			} catch (IOException e) {
				Toast.makeText(MainActivity.this, R.string.conn_error, Toast.LENGTH_LONG).show();
				e.printStackTrace();
			} catch (UserRecoverableAuthException userAuthEx) {
				if (userAuthEx != null)
					startActivityForResult(
							userAuthEx.getIntent(),
							1);
				else
					Toast.makeText(MainActivity.this, R.string.login_error, Toast.LENGTH_LONG).show();
			} catch (GoogleAuthException e) {
				Toast.makeText(MainActivity.this, R.string.login_error, Toast.LENGTH_LONG).show();
				e.printStackTrace();
			}
			return null;
		}

		@Override
		protected void onPostExecute(Void result) {

			SharedPreferences pref = getSharedPreferences("Login", Context.MODE_PRIVATE);

			Editor edit = pref.edit();
			edit.putString("user", name);
			edit.putString("token", token);
			edit.commit();

			new connect().execute();

		}
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

	@Override
	public boolean onQueryTextSubmit(String query) {
		return false;
	}

	@Override
	public boolean onQueryTextChange(String newText) {
		if (newText.length() == 0)
			listA.getFilter().filter(null);
		else
			listA.getFilter().filter(newText);
		return false;
	}

}
