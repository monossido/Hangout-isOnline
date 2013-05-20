package com.lorenzobraghetto.hangoutsisonline.logic;

import java.util.ArrayList;
import java.util.List;

import org.jivesoftware.smack.Roster;
import org.jivesoftware.smack.RosterEntry;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;

import android.content.Context;
import android.content.SharedPreferences;

public class XMPPConnect {

	public static List<Friend> XMPPgetFriends(Context context) {
		List<Friend> friends = new ArrayList<Friend>();
		XMPPConnection connection = new XMPPConnection("gmail.com"); //Server is gmail.com for Google Talk.
		SharedPreferences pref = context.getSharedPreferences("Login", Context.MODE_PRIVATE);
		try {
			connection.connect();
			connection.login(pref.getString("user", ""), pref.getString("password", "")); //Username and password.
		} catch (XMPPException e) {
			e.printStackTrace();
			return null;
		}
		try {
			Thread.sleep(5000);

			Roster roasters = connection.getRoster();
			for (RosterEntry r : roasters.getEntries()) {
				String user = r.getUser();
				if (user != null) {
					if (r.getName() != null)
						friends.add(new Friend(r.getName(), roasters.getPresence(user)));
					else
						friends.add(new Friend(user, roasters.getPresence(user)));

				}
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		connection.disconnect();
		return friends;
	}
}
