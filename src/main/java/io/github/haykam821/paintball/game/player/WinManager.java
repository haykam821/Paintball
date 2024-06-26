package io.github.haykam821.paintball.game.player;

import io.github.haykam821.paintball.game.phase.PaintballActivePhase;
import io.github.haykam821.paintball.game.player.team.TeamEntry;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class WinManager {
	private final PaintballActivePhase phase;
	private final Object2IntOpenHashMap<TeamEntry> playerCounts = new Object2IntOpenHashMap<>();

	public WinManager(PaintballActivePhase phase) {
		this.phase = phase;
		this.playerCounts.defaultReturnValue(0);
	}

	private Text getNoWinnersMessage() {
		return Text.translatable("text.paintball.no_winners").formatted(Formatting.GOLD);
	}

	private Text getWinningTeamMessage(TeamEntry team) {
		return Text.translatable("text.paintball.win", team.getName()).formatted(Formatting.GOLD);
	}

	public Text getWin() {
		this.playerCounts.clear();
		for (PlayerEntry entry : this.phase.getPlayers()) {
			if (entry.getTeam() != null && entry.isAlive()) {
				this.playerCounts.addTo(entry.getTeam(), 1);
			}
		}

		// No teams means no players
		if (this.playerCounts.isEmpty()) {
			return this.getNoWinnersMessage();
		}

		if (this.phase.isSingleplayer()) {
			return null;
		}

		TeamEntry winningTeam = null;
		for (Object2IntMap.Entry<TeamEntry> entry : this.playerCounts.object2IntEntrySet()) {
			if (entry.getIntValue() > 0) {
				if (winningTeam != null) return null;
				winningTeam = entry.getKey();
			}
		}

		return this.getWinningTeamMessage(winningTeam);
	}

	@Override
	public String toString() {
		return "WinManager{phase=" + this.phase + "}";
	}
}
