package com.freedomclient.module.hud;

import com.freedomclient.hud.HudModule;
import com.freedomclient.hud.HudPosition;
import com.freedomclient.setting.BooleanSetting;
import com.freedomclient.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.numbers.NumberFormat;
import net.minecraft.network.chat.numbers.StyledFormat;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerScoreEntry;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Scoreboard: la tabla lateral del servidor, movible y escalable en el editor del HUD y sin los números rojos. */
public class ScoreboardHud extends HudModule {
	private static final int LINE = 9;
	private static final Comparator<PlayerScoreEntry> ORDER = Comparator.comparing(PlayerScoreEntry::value).reversed()
			.thenComparing(PlayerScoreEntry::owner, String.CASE_INSENSITIVE_ORDER);

	private final BooleanSetting hideNumbers = add(new BooleanSetting("Hide red numbers", "Hide the score numbers on the right.", true));
	private final NumberSetting background = add(new NumberSetting("Background", "Opacity of the background.", 30, 0, 100, 5, "%"));
	private final BooleanSetting shadow = add(new BooleanSetting("Text shadow", "Draw a shadow under the text.", false));

	private record Line(Component name, Component score) {
	}

	public ScoreboardHud() {
		super("Scoreboard", "Move and resize the server scoreboard and hide its red numbers.", true,
				new HudPosition(HudPosition.Anchor.START, 2, HudPosition.Anchor.CENTER, -28));
	}

	/** Si el scoreboard de vanilla debe ocultarse (lo dibuja este módulo). */
	public boolean replacesVanilla() {
		return isEnabled();
	}

	private Objective objective(Minecraft client) {
		if (client.level == null || client.player == null) return null;
		Scoreboard scoreboard = client.level.getScoreboard();
		PlayerTeam team = scoreboard.getPlayersTeam(client.player.getScoreboardName());
		if (team != null) {
			DisplaySlot slot = DisplaySlot.teamColorToSlot(team.getColor());
			if (slot != null && scoreboard.getDisplayObjective(slot) != null) return scoreboard.getDisplayObjective(slot);
		}
		return scoreboard.getDisplayObjective(DisplaySlot.SIDEBAR);
	}

	private Component title(Minecraft client, boolean preview) {
		Objective objective = objective(client);
		return objective != null ? objective.getDisplayName() : preview ? Component.literal("FreedomClient") : Component.empty();
	}

	private List<Line> lines(Minecraft client, boolean preview) {
		List<Line> lines = new ArrayList<>();
		Objective objective = objective(client);
		if (objective == null) {
			if (preview) {
				lines.add(new Line(Component.literal("Kills: 12"), Component.literal("3")));
				lines.add(new Line(Component.literal("Deaths: 2"), Component.literal("2")));
				lines.add(new Line(Component.literal("play.server.net"), Component.literal("1")));
			}
			return lines;
		}
		Scoreboard scoreboard = objective.getScoreboard();
		NumberFormat format = objective.numberFormatOrDefault(StyledFormat.SIDEBAR_DEFAULT);
		scoreboard.listPlayerScores(objective).stream().filter(entry -> !entry.isHidden()).sorted(ORDER).limit(15).forEach(entry -> {
			Component name = entry.display() != null ? entry.display()
					: PlayerTeam.formatNameForTeam(scoreboard.getPlayersTeam(entry.owner()), entry.ownerName());
			lines.add(new Line(name, entry.formatValue(format)));
		});
		return lines;
	}

	@Override
	public boolean shouldRender(Minecraft client) {
		return objective(client) != null;
	}

	@Override
	public int getWidth(Minecraft client, boolean preview) {
		int width = client.font.width(title(client, preview));
		for (Line line : lines(client, preview)) {
			int lineWidth = client.font.width(line.name());
			if (!hideNumbers.get()) lineWidth += client.font.width(": ") + client.font.width(line.score());
			width = Math.max(width, lineWidth);
		}
		return width + 4;
	}

	@Override
	public int getHeight(Minecraft client, boolean preview) {
		return (lines(client, preview).size() + 1) * LINE + 2;
	}

	@Override
	public void render(GuiGraphics graphics, Minecraft client, boolean preview) {
		int width = getWidth(client, preview);
		List<Line> lines = lines(client, preview);
		int alpha = Math.round(background.getFloat() / 100.0F * 255.0F);
		int titleAlpha = Math.min(255, alpha + 40);
		graphics.fill(0, 0, width, LINE + 1, titleAlpha << 24);
		graphics.fill(0, LINE + 1, width, (lines.size() + 1) * LINE + 2, alpha << 24);

		Component title = title(client, preview);
		graphics.drawString(client.font, title, (width - client.font.width(title)) / 2, 1, 0xFFFFFFFF, shadow.get());
		int y = LINE + 2;
		for (Line line : lines) {
			graphics.drawString(client.font, line.name(), 2, y, 0xFFFFFFFF, shadow.get());
			if (!hideNumbers.get()) {
				graphics.drawString(client.font, line.score(), width - 2 - client.font.width(line.score()), y, 0xFFFF5555, shadow.get());
			}
			y += LINE;
		}
	}
}
