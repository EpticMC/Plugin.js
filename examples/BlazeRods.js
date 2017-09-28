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
 * Use blaze rods to throw fireballs
 * - - -
 * This script will allow players to 
 * left click with a blaze rod to throw 
 * fireballs.
 */

function onPlayerInteract(e) {
    var player = e.getPlayer();
    var item = player.getItemInHand();
    if (e.getAction() == Action.LEFT_CLICK_BLOCK || e.getAction() == Action.LEFT_CLICK_AIR) {
        if (item.getType() == Material.BLAZE_ROD) {
            log.info("Player left clicked with blaze rod.");
            var fb = player.getWorld().spawnEntity(player.getEyeLocation(), EntityType.SMALL_FIREBALL);
            fb.setShooter(player);
            var count = item.getAmount();
            if (count == 1) player.setItemInHand(new ItemStack(Material.AIR, 0));
            else item.setAmount(item.getAmount() - 1);
            e.setCancelled(true);
        }
    }
}

plugin.registerEvent(PlayerInteractEvent, onPlayerInteract);
