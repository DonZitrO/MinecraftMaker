package com.minecade.minecraftmaker;

import java.util.ArrayList;
import java.util.UUID;

import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.session.ClipboardHolder;
import com.sk89q.worldedit.session.SessionKey;
import com.sk89q.worldedit.session.SessionOwner;
import com.sk89q.worldedit.util.auth.AuthorizationException;

public class MakerSchematic implements SessionOwner, SessionKey {
	
	boolean active = false;
	UUID key = UUID.randomUUID();
	ArrayList<Clipboard> chunks = new ArrayList<Clipboard>();
	
	
	public boolean pasteSchematic() {
		LocalSession session = WorldEdit.getInstance().getSessionManager().get(this);
		//session.setClipboard(new ClipboardHolder(clipboard, worldData));
		return true;
	}

	@Override
	public void checkPermission(String arg0) throws AuthorizationException {
	}

	@Override
	public String[] getGroups() {
		return new String[0];
	}

	@Override
	public boolean hasPermission(String arg0) {
		return true;
	}

	@Override
	public SessionKey getSessionKey() {
		return this;
	}

	@Override
	public UUID getUniqueId() {
		return key;
	}

	@Override
	public String getName() {
		return null;
	}

	@Override
	public boolean isActive() {
		return active;
	}

	@Override
	public boolean isPersistent() {
		return false;
	}

}
