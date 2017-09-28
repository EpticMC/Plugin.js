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
 * Award WitherBoss drops
 * - - -
 * example of awarding drops to the 
 * player who does the most damage 
 * to a wither boss.
 */

var witherDamageMap = new java.util.WeakHashMap();

function onEntityDamageByEntity(e) {
    var player = e.getDamager();
    if (e.getEntityType() == EntityType.WITHER &&
        player.getType() == EntityType.PLAYER) {
        var wither = e.getEntity();
        var damageMap = witherDamageMap.get(wither);
        if (damageMap == null) {
            damageMap = new java.util.WeakHashMap();
            witherDamageMap.put(wither, damageMap);
        }
        var damage = e.getDamage();
        if (damageMap.containsKey(player)) damage += damageMap.get(player);
        damageMap.put(player, damage);
    }
}

function onEntityDeath(e) {
    if (e.getEntityType() == EntityType.WITHER) {
        var wither = e.getEntity();
        var damageMap = witherDamageMap.get(wither);
        if (damageMap == null) return; // Do normal drops.
        var winner = null;
        var maxDamage = 0;
        var i = damageMap.entrySet().iterator();
        while (i.hasNext()) {
            var entry = i.next();
            // Does not deal with ties.
            if (entry.getValue() > maxDamage) {
                winner = entry.getKey();
                maxDamage = entry.getValue();
            }
        }
        if (!winner) return; // Do normal drops.
        var drops = new java.util.ArrayList(e.getDrops());
        e.getDrops().clear();
        i = damageMap.entrySet().iterator();
        while (i.hasNext()) {
            var entry = i.next();
            if (entry.getKey() == winner) winner.sendMessage("You did the most damage!");
            else entry.getKey().sendMessage(winner.getDisplayName() + " made the most damage!");
        }
        // Try to put items in the winner's inventory, if that fails
        // Add them back to the wither's drops.
        for (i = 0; i < drops.size(); i++) e.getDrops().addAll(winner.getInventory().addItem(drops.get(i)).values());
    }
}

plugin.registerEvent(EntityDamageByEntityEvent, onEntityDamageByEntity);
plugin.registerEvent(EntityDeathEvent, onEntityDeath);

