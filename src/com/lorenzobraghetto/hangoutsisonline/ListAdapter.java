package com.lorenzobraghetto.hangoutsisonline;

import java.util.List;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.lorenzobraghetto.hangoutsisonline.logic.Friend;

public class ListAdapter extends BaseAdapter {

	private LayoutInflater mLayoutInflater;
	private List<Friend> mFriends;
	private Context mContext;

	public ListAdapter(Context context, List<Friend> friends) {
		mLayoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		mFriends = friends;
		mContext = context;
	}

	private class ViewHolder {
		public TextView name;
		public TextView presence;
		public ImageView status;
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
		ViewHolder viewHolder = null;
		if (convertView == null) {
			LayoutInflater inflater = (LayoutInflater) mContext
					.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			convertView = inflater.inflate(R.layout.listrow, null);
			viewHolder = new ViewHolder();
			viewHolder.name = (TextView) convertView.findViewById(R.id.name);
			viewHolder.presence = (TextView) convertView.findViewById(R.id.presence);
			viewHolder.status = (ImageView) convertView.findViewById(R.id.status);
			convertView.setTag(viewHolder);
		} else {
			viewHolder = (ViewHolder) convertView.getTag();
		}
		Friend friend = getItem(position);
		viewHolder.name.setText(friend.getUser());
		viewHolder.presence.setText(friend.getPresence().toString());
		if (friend.getPresence().toString().contains("unavailable"))
			viewHolder.status.setImageResource(android.R.drawable.presence_offline);
		else if (friend.getPresence().toString().contains("away"))
			viewHolder.status.setImageResource(android.R.drawable.presence_away);
		else if (friend.getPresence().toString().startsWith("available"))
			viewHolder.status.setImageResource(android.R.drawable.presence_online);

		return convertView;
	}

}
