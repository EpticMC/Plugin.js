importPackage(org.bukkit)
importPackage(org.bukkit.entity)
importPackage(org.bukkit.inventory)
importPackage(org.bukkit.event)
importPackage(org.bukkit.event.player)
importPackage(org.bukkit.event.block)
importPackage(org.bukkit.event.entity)
importPackage(org.bukkit.util)
importPackage(org.bukkit.potion)

/* 
 * Disable Monster Spawners
 * - - -
 * Quick hack to stop monster        
 * spawners from working server wide
 */


function onCreatureSpawn(e) {
    if (e.getSpawnReason() == CreatureSpawnEvent.SpawnReason.SPAWNER) e.setCancelled(true);
}

plugin.registerEvent(CreatureSpawnEvent, onCreatureSpawn);
