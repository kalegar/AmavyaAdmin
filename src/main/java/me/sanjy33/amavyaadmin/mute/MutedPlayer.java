package me.sanjy33.amavyaadmin.mute;

import java.util.UUID;

public class MutedPlayer {
	private UUID player;
	private String reason;
	private final Muter muter;
	private long unMuteTime;
	
	public MutedPlayer(UUID player, Muter muter, String reason, long unMuteTime) {
		this.player = player;
		this.muter = muter;
		this.reason = reason;
		this.unMuteTime = unMuteTime;
	}

	public UUID getPlayer() {
		return player;
	}

	public void setPlayer(UUID player) {
		this.player = player;
	}

	public String getReason() {
		return reason;
	}

	public void setReason(String reason) {
		this.reason = reason;
	}

	public Muter getMuter() {
		return muter;
	}

	public long getUnMuteTime() {
		return unMuteTime;
	}

	public void setUnMuteTime(long unMuteTime) {
		this.unMuteTime = unMuteTime;
	}
	
	@Override
	public boolean equals(Object o) {
		if (o instanceof MutedPlayer) {
			MutedPlayer m = (MutedPlayer) o;
			return this.player.equals(m.player);
		}
		return false;
	}

	public static class Muter {

		private final String name;
		private final UUID uuid;
		private final boolean console;

		public static Muter console() {
			return new Muter();
		}

		private Muter() {
			this.name = "CONSOLE";
			this.uuid = null;
			this.console = true;
		}

		public Muter(String name, UUID uuid) {
			this.name = name;
			this.uuid = uuid;
			this.console = false;
		}

		public boolean isConsole() {
			return console;
		}

		public String getName() {
			return name;
		}

		public UUID getUuid() {
			return uuid;
		}
	}

}
