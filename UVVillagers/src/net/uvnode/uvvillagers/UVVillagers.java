/**
 * 
 */
package net.uvnode.uvvillagers;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;


/**
 * @author James Cornwell-Shiel
 *
 */
public final class UVVillagers extends JavaPlugin implements Listener {
	
	UVSiege activeSiege = null;
	
	@Override
	public void onEnable() {
		getServer().getPluginManager().registerEvents(this, this);
		
		//Start timer
		getServer().getScheduler().runTaskTimerAsynchronously(this, new Runnable() {

			@Override
			public void run() {
				// TODO Auto-generated method stub
				List<World> worlds = getServer().getWorlds();
				for (int i = 0; i < worlds.size(); i++) {
					if (worlds.get(i).getTime() >= 0 && worlds.get(i).getTime() < 20) {
						UVTimeEvent event = new UVTimeEvent(worlds.get(i), UVTimeEventType.DAWN);
						getServer().getPluginManager().callEvent(event);
						//getServer().broadcastMessage(event.getMessage());
					}
				}
			}
		
		}, 0, 20);
	}
	
	@Override
	public void onDisable() {
		
	}
	
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args){
		if(cmd.getName().equalsIgnoreCase("siege")){
			if (activeSiege == null) {
				sender.sendMessage("No sieges.");
			}
			else {
				activeSiege.sendOverview(sender);
			}
			return true;
		}
		return false; 
	}

	@EventHandler
	public void creatureSpawn(CreatureSpawnEvent event) {
		int x, y, z;
		switch(event.getSpawnReason()) {
			case VILLAGE_DEFENSE:
				x = event.getEntity().getLocation().getBlockX();
				y = event.getEntity().getLocation().getBlockY();
				z = event.getEntity().getLocation().getBlockZ();
				getLogger().info("CreatureSpawnEvent " + event.getEntityType().getName() + " VILLAGE_DEFENSE @ " + x + "," + y + "," + z + "!");
				break;
			case VILLAGE_INVASION:
				x = event.getEntity().getLocation().getBlockX();
				y = event.getEntity().getLocation().getBlockY();
				z = event.getEntity().getLocation().getBlockZ();
				getLogger().info("CreatureSpawnEvent " + event.getEntityType().getName() + " VILLAGE_INVASION @ " + x + "," + y + "," + z + "!");
				if (activeSiege == null) {
					getServer().broadcastMessage("A zombie siege has begun!!!");
					activeSiege = new UVSiege(event.getEntity().getWorld().getTime());
				}
				activeSiege.addSpawn(event.getEntity());
				break;
			default:
				break;
		}
	}
	
	@EventHandler
	public void entityDeath(EntityDeathEvent event) {
		if (activeSiege != null) {
			if (activeSiege.checkEntityId(event.getEntity().getEntityId())) {
				activeSiege.addPlayerKill(event.getEntity().getKiller().getName());
			}
		}
	}
	
	@EventHandler
	public void emeraldsAtDawn(UVTimeEvent event) {
		switch(event.getType()) {
			case DAWN:
				Collection<Villager> villagers = event.getWorld().getEntitiesByClass(org.bukkit.entity.Villager.class);
				List<Player> players = event.getWorld().getPlayers();
				Map<String, Integer> playerRewards = new HashMap<String, Integer>();
				Iterator<Villager> villagerIterator = villagers.iterator();
				while (villagerIterator.hasNext()) {
					Villager v = villagerIterator.next();
					for (int i = 0; i < players.size(); i++) {
						if (players.get(i).getLocation().distanceSquared(v.getLocation()) < 4096.0) {
							if (playerRewards.get(players.get(i).getName()) != null) {
								playerRewards.put(players.get(i).getName(), playerRewards.get(players.get(i).getName()) + 1);
							} else {
								playerRewards.put(players.get(i).getName(), 1);
							}
						}
					}
				}
				
				Iterator<String> prs = playerRewards.keySet().iterator();
				while (prs.hasNext()) {
					String pname = prs.next();
					int numNPCs = playerRewards.get(pname);
					int amount = 0;
					getLogger().info(pname + " is close to " + numNPCs + " villagers.");
					Random rng = new Random();
					if (activeSiege != null) {
						int kills = activeSiege.getPlayerKills(pname);
						for (int i = 0; i < kills; i++) {
							amount += rng.nextInt(2);
						}
					}
					if (numNPCs > 20) {
						for (int i = 0; i < (int)(numNPCs / 20); i++) {
							if (activeSiege == null) {
								amount += rng.nextInt(3);
							} else {
								amount += rng.nextInt(3) + 1;								
							}
						}
						if (amount > 0) {
							ItemStack items = new ItemStack(Material.EMERALD, amount);
							getServer().getPlayer(pname).getInventory().addItem(items);
							getServer().getPlayer(pname).sendMessage("Grateful villagers gave you " + amount + " emeralds!");
						}
						else
							getServer().getPlayer(pname).sendMessage("The villagers didn't have any emeralds for you today.");

						getLogger().info(pname + " received " + amount + " emeralds.");						
					}
				}
				activeSiege = null;
				break;
			default:
				break;
		}
	}
	
}
