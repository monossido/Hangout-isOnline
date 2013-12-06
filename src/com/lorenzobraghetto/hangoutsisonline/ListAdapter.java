package com.lorenzobraghetto.hangoutsisonline;

import java.util.ArrayList;
import java.util.List;

import org.jivesoftware.smack.packet.Presence;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.ContactsContract.QuickContact;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.ImageView;
import android.widget.QuickContactBadge;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.lorenzobraghetto.hangoutsisonline.logic.Friend;

public class ListAdapter extends BaseAdapter {

	private List<Friend> mFriends;
	private Context mContext;
	private ViewHolder viewHolder;
	private List<Bitmap> friendsContactPicture;
	private List<Uri> friendsContactUri;
	private PlanetFilter planetFilter;
	private List<Friend> mFriendsCopy;
	private List<Uri> friendsContactUriCopy;
	private List<Bitmap> friendsContactPictureCopy;

	public ListAdapter(Context context, List<Friend> friends, List<Uri> friendsContactUri, List<Bitmap> friendsContactPicture) {
		mFriends = friends;
		mContext = context;
		this.friendsContactUri = friendsContactUri;
		this.friendsContactPicture = friendsContactPicture;
		this.mFriendsCopy = friends;
		this.friendsContactUriCopy = friendsContactUri;
		this.friendsContactPictureCopy = friendsContactPicture;

	}

	private class ViewHolder {
		public RelativeLayout listrow;
		public TextView name;
		public TextView presence;
		public ImageView status;
		public QuickContactBadge quickContact;
	}

	@Override
	public int getCount() {
		return mFriends.size();
	}

	@Override
	public Friend getItem(int position) {
		return mFriends.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		viewHolder = null;
		if (convertView == null) {
			LayoutInflater inflater = (LayoutInflater) mContext
					.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			convertView = inflater.inflate(R.layout.listrow, null);
			viewHolder = new ViewHolder();
			viewHolder.listrow = (RelativeLayout) convertView.findViewById(R.id.listrow);
			viewHolder.name = (TextView) convertView.findViewById(R.id.name);
			viewHolder.presence = (TextView) convertView.findViewById(R.id.presence);
			viewHolder.status = (ImageView) convertView.findViewById(R.id.status);
			viewHolder.quickContact = (QuickContactBadge) convertView.findViewById(R.id.quickbadge);
			convertView.setTag(viewHolder);
		} else {
			viewHolder = (ViewHolder) convertView.getTag();
		}
		Friend friend = getItem(position);

		viewHolder.name.setText(friend.getUser());

		processText(friend.getPresence());

		if (position <= friendsContactUri.size()) {
			final Uri contactUri = friendsContactUri.get(position);
			viewHolder.quickContact.assignContactUri(contactUri);
			if (contactUri != null)
				viewHolder.listrow.setOnClickListener(new OnClickListener() {

					@Override
					public void onClick(View v) {
						try {
							QuickContact.showQuickContact(mContext, viewHolder.quickContact, contactUri, QuickContact.MODE_MEDIUM, null);
						} catch (ActivityNotFoundException e) {
							e.printStackTrace();
						}
					}
				});
			Bitmap picture = friendsContactPicture.get(position);

			if (picture != null)
				viewHolder.quickContact.setImageBitmap(picture);
			else
				viewHolder.quickContact.setImageResource(R.drawable.ic_contact_picture);
		}
		return convertView;
	}

	public Filter getFilter() {
		if (planetFilter == null)
			planetFilter = new PlanetFilter();

		return planetFilter;
	}

	private void processText(Presence presence) {

		if (!presence.isAvailable()) {
			viewHolder.status.setImageResource(android.R.drawable.presence_offline);
			viewHolder.presence.setText(presence.toString());
		}
		else if (presence.isAway()) {
			viewHolder.status.setImageResource(android.R.drawable.presence_away);
			if (presence.getFrom().contains("Messaging")) {
				viewHolder.presence.setText(mContext.getResources().getString(R.string.hangouts_mobile));
			} else if (presence.getFrom().contains("messaging")) {
				viewHolder.presence.setText(mContext.getResources().getString(R.string.hangouts_pc_away));
			} else {
				viewHolder.presence.setText(mContext.getResources().getString(R.string.gtalk_away));
			}
		} else if (presence.isAvailable()) {
			viewHolder.status.setImageResource(android.R.drawable.presence_online);
			if (presence.getFrom().contains("messaging"))
				viewHolder.presence.setText(mContext.getResources().getString(R.string.hangouts_pc_online));
			else
				viewHolder.presence.setText(mContext.getResources().getString(R.string.gtalk_online));
		}
	}

	private class PlanetFilter extends Filter {
		@Override
		protected FilterResults performFiltering(CharSequence constraint) {
			FilterResults results = new FilterResults();
			if (constraint == null || constraint.length() == 0) {
				results.values = mFriendsCopy;
				results.count = mFriendsCopy.size();
				friendsContactUri = friendsContactUriCopy;
				friendsContactPicture = friendsContactPictureCopy;
			}
			else {
				List<Friend> nFriendList = new ArrayList<Friend>();
				List<Uri> nContactUriList = new ArrayList<Uri>();
				List<Bitmap> nContactPictureList = new ArrayList<Bitmap>();

				for (int i = 0; i < mFriendsCopy.size(); i++) {
					Friend f = mFriendsCopy.get(i);
					if (f.getUser().toUpperCase().contains((constraint.toString().toUpperCase()))) {
						nFriendList.add(f);
						nContactUriList.add(friendsContactUriCopy.get(i));
						nContactPictureList.add(friendsContactPictureCopy.get(i));
					}
				}

				results.values = nFriendList;
				results.count = nFriendList.size();

				//change badge
				friendsContactUri = nContactUriList;
				friendsContactPicture = nContactPictureList;

			}
			return results;
		}

		@Override
		protected void publishResults(CharSequence constraint, FilterResults results) {
			mFriends = (List<Friend>) results.values;
			notifyDataSetChanged();
		}

	}

}
