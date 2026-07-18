package com.wqlfz.xaeromultiworld;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.lang.reflect.Method;
import java.util.Objects;
import java.util.Random;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bstats.bukkit.Metrics;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRegisterChannelEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;

public class XaeroMultiWorldPlugin extends JavaPlugin implements Listener {
	private static final String WORLDMAP_CHANNEL = "xaeroworldmap:main";
	private static final String MINIMAP_CHANNEL = "xaerominimap:main";

	public static final Logger log = Logger.getLogger(XaeroMultiWorldPlugin.class.getName());

	private static final boolean FOLIA = hasClass("io.papermc.paper.threadedregions.RegionizedServer");

	private int serverLevelId;

	@Override
	public void onEnable() {
		this.serverLevelId = this.initializeServerLevelId();

		this.getServer().getMessenger().registerOutgoingPluginChannel(this, WORLDMAP_CHANNEL);
		this.getServer().getMessenger().registerOutgoingPluginChannel(this, MINIMAP_CHANNEL);
		this.getServer().getPluginManager().registerEvents(this, this);

		try {
			new Metrics(this, 32708);
		} catch (Throwable ignored) { }

		for (Player player : this.getServer().getOnlinePlayers()) {
			this.sendPlayerWorldIdToChannels(player);
		}
	}

	@Override
	public void onDisable() {
		this.getServer().getMessenger().unregisterOutgoingPluginChannel(this);
	}

	@EventHandler
	public void onPlayerRegisterChannel(PlayerRegisterChannelEvent event) {
		String channel = event.getChannel();
		if (!channel.equals(WORLDMAP_CHANNEL) &&
			!channel.equals(MINIMAP_CHANNEL)) {
			return;
		}

		this.sendPlayerWorldId(event.getPlayer(), channel);
	}

	@EventHandler
	public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
		this.sendPlayerWorldIdToChannels(event.getPlayer());
	}

	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent event) {
		final Player player = event.getPlayer();

		this.sendPlayerWorldIdToChannels(player);
		this.runDelayed(player, () -> this.sendPlayerWorldIdToChannels(player), 20L);
		this.runDelayed(player, () -> this.sendPlayerWorldIdToChannels(player), 60L);
	}

	private void sendPlayerWorldId(Player player, String channel) {
		ByteArrayDataOutput bytes = ByteStreams.newDataOutput();
		bytes.writeByte(0);
		bytes.writeInt(this.serverLevelId);

		player.sendPluginMessage(this, channel, bytes.toByteArray());
	}

	private void sendPlayerWorldIdToChannels(Player player) {
		this.sendPlayerWorldId(player, WORLDMAP_CHANNEL);
		this.sendPlayerWorldId(player, MINIMAP_CHANNEL);
	}

	private void runDelayed(Player player, final Runnable task, long delayTicks) {
		if (!FOLIA) {
			this.getServer().getScheduler().runTaskLater(this, task, delayTicks);
			return;
		}

		try {
			Object scheduler = Player.class.getMethod("getScheduler").invoke(player);
			Class<?> schedulerClass = Class.forName("io.papermc.paper.threadedregions.scheduler.EntityScheduler");
			Method runDelayed = schedulerClass.getMethod("runDelayed", Plugin.class, Consumer.class, Runnable.class, long.class);
			Consumer<Object> scheduledTask = ignored -> task.run();
			runDelayed.invoke(scheduler, this, scheduledTask, null, delayTicks);
		} catch (Exception ex) {
			log.log(Level.WARNING, "Failed to schedule delayed task on Folia", ex);
		}
	}

	private static boolean hasClass(String className) {
		try {
			Class.forName(className);
			return true;
		} catch (ClassNotFoundException ex) {
			return false;
		}
	}

	private int initializeServerLevelId() {
		try {
			return this.generateDeterministicServerLevelId();
		} catch (Exception ex) {
			log.log(Level.WARNING, "Failed to generate deterministic world ID, falling back to xaeromap.txt", ex);
		}

		try {
			String worldFolder = getServer().getWorldContainer().getCanonicalPath();
			File xaeromapFile = new File(worldFolder + File.separator + "xaeromap.txt");
			if (!xaeromapFile.exists()) {
				try {
					try (FileOutputStream xaeromapFileStream = new FileOutputStream(xaeromapFile, false)) {
						int id = (new Random()).nextInt();
						String idString = "id:" + id;
						xaeromapFileStream.write(idString.getBytes());

						return id;
					}
				} catch (java.io.IOException | SecurityException ex) {
					log.log(Level.WARNING, "Failed to create xaeromap.txt", ex);
				}
			} else {
				try (FileReader fileReader = new FileReader(xaeromapFile);
					 BufferedReader bufferedReader = new BufferedReader(fileReader)) {
					String line = bufferedReader.readLine();
					String[] args = line.split(":");
					if (!Objects.equals(args[0], "id")) {
						throw new Exception("Failed to read id from xaeromap.txt");
					}

					return Integer.parseInt(args[1]);
				} catch (java.io.IOException | NumberFormatException | ArrayIndexOutOfBoundsException ex) {
					log.log(Level.WARNING, "Failed to read xaeromap.txt", ex);
				}
			}
		} catch (Exception ex) {
			log.log(Level.WARNING, "Failed to get world ID", ex);
		}

		return 0;
	}

	private int generateDeterministicServerLevelId() throws Exception {
		String canonicalWorldPath = this.getServer().getWorldContainer().getCanonicalPath();
		int serverPort = this.getServer().getPort();
		return Objects.hash(canonicalWorldPath, serverPort);
	}
}
