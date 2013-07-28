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
import android.util.Log;
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
import com.lorenzobraghetto.hangoutsisonline.logic.CallBack;
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
	public boolean loading;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		loading = false;
		rebuilded = false;

		progress = (ProgressBar) findViewById(R.id.progress);
		listV = (ListView) findViewById(R.id.list);

		pref = getSharedPreferences("Login", Context.MODE_PRIVATE);

		if (pref.getString("token", "").equals("")) {
			int result = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
			if (result == ConnectionResult.SUCCESS) {
				Intent intent = AccountPicker.newChooseAccountIntent(null, null, new String[] { "com.google" },
						false, null, null, null, null);
				startActivityForResult(intent, 1);
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

	class connect extends AsyncTask<Void, Integer, List<Friend>> {

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			loading = true;
			supportInvalidateOptionsMenu();
			progress.setVisibility(View.VISIBLE);
			listV.setVisibility(View.GONE);

		}

		@Override
		protected void onProgressUpdate(Integer... values) {
			super.onProgressUpdate(values);
			if (values[0] == 1)
				Toast.makeText(MainActivity.this, R.string.on_connect, Toast.LENGTH_SHORT).show();
			else if (values[0] == 2)
				Toast.makeText(MainActivity.this, R.string.on_friends, Toast.LENGTH_SHORT).show();
		}

		class CallBackImpl implements CallBack {

			@Override
			public void onConnect() {
				publishProgress(1);
			}

			@Override
			public void onDownloadFriends() {
				publishProgress(2);
			}
		}

		@Override
		protected List<Friend> doInBackground(Void... params) {
			friends = XMPPConnect.XMPPgetFriends(MainActivity.this, new CallBackImpl());
			if (friends != null) {
				sortFriendsByStatus();
				getBadge();
			}
			return friends;
		}

		@Override
		protected void onPostExecute(List<Friend> friends) {
			super.onPostExecute(friends);
			loading = false;
			supportInvalidateOptionsMenu();
			progress.setVisibility(View.GONE);
			listV.setVisibility(View.VISIBLE);

			if (friends == null) {
				if (rebuilded)
					Toast.makeText(MainActivity.this, R.string.login_error, Toast.LENGTH_LONG).show();
				else {
					rebuilded = true;
					String name = pref.getString("user", "");
					if (name.length() > 0)
						new AuthTask().execute(new String[] { name });
				}
			} else {
				listA = new ListAdapter(MainActivity.this, friends, friendsContactUri, friendsContactPicture);
				listV.setAdapter(listA);
			}

		}
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		if (loading) {
			menu.getItem(0).setEnabled(false);
			menu.getItem(1).setEnabled(false);
			menu.getItem(2).setEnabled(false);
			menu.getItem(4).setEnabled(false);
		} else {
			menu.getItem(0).setEnabled(true);
			menu.getItem(1).setEnabled(true);
			menu.getItem(2).setEnabled(true);
			menu.getItem(4).setEnabled(true);
		}
		return true;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getSupportMenuInflater().inflate(R.menu.main, menu);
		SearchView searchView = (SearchView) menu.findItem(R.id.action_search).getActionView();
		searchView.setOnQueryTextListener(this);
		return true;
	}

	public void sortFriendsAlphabetically(List<Friend> friends) {
		if (friends != null) {
			Collections.sort(friends, new Comparator<Friend>() {
				@Override
				public int compare(final Friend object1, final Friend object2) {
					return object1.getUser().compareTo(object2.getUser());
				}
			});
		}
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
			startActivityForResult(intent, 1);
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
			if (listA != null && friends != null) {
				if (item.isChecked()) {
					item.setChecked(false);
					sortFriendsAlphabetically(friends);
				} else {
					item.setChecked(true);
					sortFriendsByStatus();
				}
				listA.notifyDataSetChanged();
			}
			break;
		}
		return true;
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		Log.v("HANGOUTS", "requestCode=" + requestCode + ", requestCode" + requestCode);
		if (requestCode == 1 && resultCode == RESULT_OK) {
			String accountName = data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
			if (accountName.length() > 0)
				new AuthTask().execute(new String[] { accountName });
		}
	}

	private class AuthTask extends AsyncTask<String, Void, String> {

		private String name;
		private String token;

		@Override
		protected String doInBackground(String... params) {
			name = params[0];
			try {
				token = GoogleAuthUtil.getToken(MainActivity.this, name, "oauth2:" + mScope);
			} catch (IOException e) {
				e.printStackTrace();
				return "conn";
			} catch (UserRecoverableAuthException userAuthEx) {
				if (userAuthEx != null && userAuthEx.getIntent() != null)
					startActivityForResult(
							userAuthEx.getIntent(),
							1);
				else
					return "login";
			} catch (GoogleAuthException e) {
				e.printStackTrace();
				return "login";
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
				return "login";
			}
			return null;
		}

		@Override
		protected void onPostExecute(String exception) {
			Log.v("HANGOUTS", "exception=" + exception + ", user" + name + " token=" + token);

			if (exception == null) {
				SharedPreferences pref = getSharedPreferences("Login", Context.MODE_PRIVATE);

				Editor edit = pref.edit();
				edit.putString("user", name);
				edit.putString("token", token);
				edit.commit();

				new connect().execute();
			} else if (exception.equals("login"))
				Toast.makeText(MainActivity.this, R.string.login_error, Toast.LENGTH_LONG).show();
			else if (exception.equals("conn"))
				Toast.makeText(MainActivity.this, R.string.conn_error, Toast.LENGTH_LONG).show();

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
		if (listA != null) {
			if (newText.length() == 0)
				listA.getFilter().filter(null);
			else
				listA.getFilter().filter(newText);
		}
		return false;
	}

}
