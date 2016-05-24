package com.minecade.core.scoreboard;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.bukkit.ChatColor;

import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;

public class AnimatedText implements DynamicText {

	private final String fullText;
	private final List<String> animations;
	private final Iterator<String> cycle;

	private String visibleText;

	public AnimatedText(String fullText, String... animations) {
		this.fullText = ChatColor.translateAlternateColorCodes('&', fullText);
		if (animations != null) {
			this.animations = Lists.newArrayList(animations);
		} else {
			this.animations = new ArrayList<>();
		}
		cycle = Iterators.cycle(this.animations);
	}

	public void addColorFlashes(ChatColor color, boolean bold, int howMany) {
		String base = last();
		for (int i = 0; i < howMany; i++) {
			addColorChange(color, bold);
			animations.add(base);
		}
	}

	public void addColorChange(ChatColor color, boolean bold) {
		String text = ChatColor.stripColor(last());
		if (bold) {
			text = ChatColor.BOLD + text;
		}
		text = color + text;
		this.animations.add(text);
	}

	public void addProgressiveColorChange(ChatColor color, boolean bold, ChatColor result) {
		String base = last();
		int lastColorCharIndex = -2;
		Character formatChar1 = null;
		Character formatChar2 = null;
		for (int i = 0; i < base.length(); i++) {
			// color char detected
			if (ChatColor.COLOR_CHAR == base.charAt(i)) {
				lastColorCharIndex = i;
				continue;
			}
			// color char code
			if (i == lastColorCharIndex + 1) {
				formatChar1 = formatChar2;
				formatChar2 = base.charAt(i);
				continue;
			}
			String text = "";
			if (bold) {
				text = ChatColor.BOLD + text;
			}
			text = color + text;
			text = insert(base, text, i, formatChar1, formatChar2);
			if (result != null) {
				if (bold) {
					text = ChatColor.BOLD + text;
				}
				text = result + text;
			}
			animations.add(text);
		}
		if (result != null) {
			String text = ChatColor.stripColor(last());
			if (bold) {
				text = ChatColor.BOLD + text;
			}
			text = result + text;
			animations.add(text);
		}
	}

	@Override
	public String changeText() {
		if (cycle.hasNext()) {
			visibleText = cycle.next();
		} else {
			visibleText = fullText;
		}
		return visibleText;
	}

	private String last() {
		if (animations.size() > 0) {
			return animations.get(animations.size() - 1);
		}
		return fullText;
	}

	private String insert(String base, String insert, int index, Character format1, Character format2) {
		StringBuilder builder = new StringBuilder(ChatColor.stripColor(base.substring(0, index)));
		builder.append(insert).append(base.charAt(index));
		if (index + 1 < base.length()) {
			if (format1 != null) {
				builder.append(ChatColor.COLOR_CHAR).append(format1);
			}
			if (format2 != null) {
				builder.append(ChatColor.COLOR_CHAR).append(format2);
			}
			builder.append(base.substring(index + 1, base.length()));
		}
		return builder.toString();
	}

	@Override
	public String getOriginalText() {
		return fullText;
	}

	@Override
	public String getVisibleText() {
		if (visibleText == null) {
			visibleText = fullText;
		}
		return visibleText;
	}

}
