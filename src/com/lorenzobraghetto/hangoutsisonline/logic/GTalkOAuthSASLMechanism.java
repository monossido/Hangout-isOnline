package com.lorenzobraghetto.hangoutsisonline.logic;

import java.io.IOException;

import org.jivesoftware.smack.SASLAuthentication;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.sasl.SASLMechanism;
import org.jivesoftware.smack.util.Base64;

//taken from: http://stackoverflow.com/a/10712949/198996
//note: you don't have to change anything in this class
public class GTalkOAuthSASLMechanism extends SASLMechanism {

	public static final String NAME = "X-GOOGLE-TOKEN";

	public GTalkOAuthSASLMechanism(SASLAuthentication saslAuthentication) {
		super(saslAuthentication);
	}

	@Override
	protected String getName() {
		return NAME;
	}

	@Override
	protected void authenticate() throws IOException, XMPPException {
		final StringBuilder stanza = new StringBuilder();
		byte response[] = null;

		stanza.append("<auth xmlns=\"urn:ietf:params:xml:ns:xmpp-sasl\""
				+ "mechanism=\"X-OAUTH2\"" + "auth:service=\"oauth2\""
				+ "xmlns:auth= \"http://www.google.com/talk/protocol/auth\">");

		String composedResponse = "\0" + authenticationId + "\0" + password;
		response = composedResponse.getBytes("UTF-8");
		String authenticationText = "";
		if (response != null) {
			authenticationText = Base64.encodeBytes(response,
					Base64.DONT_BREAK_LINES);
		}

		stanza.append(authenticationText);
		stanza.append("</auth>");

		Packet authPacket = new Packet() {

			@Override
			public String toXML() {
				return stanza.toString();
			}
		};

		getSASLAuthentication().send(authPacket);
	}
}
