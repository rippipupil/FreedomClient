package com.freedomclient.freedomplayer;

import com.freedomclient.freedomplayer.LibrarySync.PlayEvent;

import java.util.List;

/** La biblioteca tal y como la usa el cliente: canciones (con su nota y su audio), playlists, rankings y escuchas. */
public record LibraryData(List<Song> songs, List<Playlist> playlists, List<Ranking> rankings, List<PlayEvent> plays) {
	public static final LibraryData EMPTY = new LibraryData(List.of(), List.of(), List.of(), List.of());

	/** Una canción. {@code audioPath} es la ruta del audio en este PC; {@code score} va de 1 a 10 (medias estrellas) o null. */
	public record Song(String id, String title, String artist, String album, int durationSec, long coverColor, String sourceUrl, long addedAt,
					   String audioPath, Integer score, int plays) {
		public Song withScore(Integer score) {
			return new Song(id, title, artist, album, durationSec, coverColor, sourceUrl, addedAt, audioPath, score, plays);
		}
	}

	public record Playlist(String id, String name, List<String> songIds, long coverColor) {
	}

	public record Ranking(String id, String name, List<String> songIds) {
	}

	public Song song(String id) {
		for (Song song : songs) {
			if (song.id().equals(id)) return song;
		}
		return null;
	}
}
