package com.freedomclient.freedomplayer;

import java.security.SecureRandom;
import java.util.Locale;
import java.util.regex.Pattern;

/** Ids de dispositivo de Syncthing («XXXXXXX-XXXXXXX-…», 8 grupos de 7) y claves de la API, como en FreedomPlayer. */
public final class SyncIds {
	private static final Pattern DEVICE = Pattern.compile("[A-Z2-7]{7}(?:-[A-Z2-7]{7}){7}");

	private SyncIds() {
	}

	/** El id leído de un QR (o escrito a mano): en mayúsculas y con guiones; null si no lo es. */
	public static String device(String text) {
		StringBuilder compact = new StringBuilder();
		for (char c : text.trim().toUpperCase(Locale.ROOT).toCharArray()) {
			if (Character.isLetterOrDigit(c)) compact.append(c);
		}
		if (compact.length() != 56) return null;
		StringBuilder id = new StringBuilder();
		for (int i = 0; i < 56; i += 7) {
			if (i > 0) id.append('-');
			id.append(compact, i, i + 7);
		}
		return DEVICE.matcher(id).matches() ? id.toString() : null;
	}

	/** Clave para la API REST, generada cada vez que se arranca Syncthing. */
	public static String newApiKey() {
		String chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
		SecureRandom random = new SecureRandom();
		StringBuilder key = new StringBuilder();
		for (int i = 0; i < 40; i++) key.append(chars.charAt(random.nextInt(chars.length())));
		return key.toString();
	}
}
