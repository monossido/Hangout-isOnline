package com.lorenzobraghetto.hangoutsisonline;

import java.util.List;

import org.jivesoftware.smack.packet.Presence;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.QuickContactBadge;
import android.widget.TextView;

import com.lorenzobraghetto.hangoutsisonline.logic.Friend;

public class ListAdapter extends BaseAdapter {

	private List<Friend> mFriends;
	private Context mContext;
	private ViewHolder viewHolder;
	private List<Bitmap> friendsContactPicture;
	private List<Uri> friendsContactUri;

	public ListAdapter(Context context, List<Friend> friends, List<Uri> friendsContactUri, List<Bitmap> friendsContactPicture) {
		mFriends = friends;
		mContext = context;
		this.friendsContactUri = friendsContactUri;
		this.friendsContactPicture = friendsContactPicture;

	}

	private class ViewHolder {
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
		Uri contactUri = friendsContactUri.get(position);
		viewHolder.quickContact.assignContactUri(contactUri);
		Bitmap picture = friendsContactPicture.get(position);
		if (picture != null)
			viewHolder.quickContact.setImageBitmap(picture);
		else
			viewHolder.quickContact.setImageResource(R.drawable.ic_contact_picture);
		return convertView;
	}

	private void processText(Presence presence) {

		if (presence.toString().contains("unavailable")) {
			viewHolder.status.setImageResource(android.R.drawable.presence_offline);
			viewHolder.presence.setText(presence.toString());
		}
		else if (presence.toString().contains("away")) {
			viewHolder.status.setImageResource(android.R.drawable.presence_away);
			if (presence.getFrom().contains("Messaging")) {
				viewHolder.presence.setText(mContext.getResources().getString(R.string.hangouts_mobile));
			} else if (presence.getFrom().contains("messaging")) {
				viewHolder.presence.setText(mContext.getResources().getString(R.string.hangouts_pc_away));
			} else {
				viewHolder.presence.setText(mContext.getResources().getString(R.string.gtalk_away));
			}
		} else if (presence.toString().startsWith("available")) {
			viewHolder.status.setImageResource(android.R.drawable.presence_online);
			if (presence.getFrom().contains("messaging"))
				viewHolder.presence.setText(mContext.getResources().getString(R.string.hangouts_pc_online));
			else
				viewHolder.presence.setText(mContext.getResources().getString(R.string.gtalk_online));
		}
	}

}
