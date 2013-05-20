package com.lorenzobraghetto.hangoutsisonline.logic;

import org.jivesoftware.smack.packet.Presence;

public class Friend {

	private String user;
	private Presence presence;

	public Friend(String user, Presence presence) {
		this.user = user;
		this.presence = presence;
	}

	public String getUser() {
		return user;
	}

	public Presence getPresence() {
		return presence;
	}
}
