package su.nightexpress.excellentchallenges.challenge.listener;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BrewingStand;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.BrewerInventory;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import su.nightexpress.excellentchallenges.ChallengesPlugin;
import su.nightexpress.excellentchallenges.Keys;
import su.nightexpress.excellentchallenges.api.event.PlayerChallengeCompleteEvent;
import su.nightexpress.excellentchallenges.api.event.PlayerChallengeObjectiveEvent;
import su.nightexpress.excellentchallenges.challenge.ChallengeCategory;
import su.nightexpress.excellentchallenges.challenge.ChallengeManager;
import su.nightexpress.excellentchallenges.challenge.GeneratedChallenge;
import su.nightexpress.excellentchallenges.config.Config;
import su.nightexpress.excellentchallenges.config.Lang;
import su.nightexpress.excellentchallenges.data.object.ChallengeUser;
import su.nightexpress.nightcore.manager.AbstractListener;
import su.nightexpress.nightcore.util.PDCUtil;

public class ChallengeListener extends AbstractListener<ChallengesPlugin> {

    public ChallengeListener(@NotNull ChallengeManager challengeManager) {
        super(challengeManager.plugin());
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onJoin(@NotNull PlayerJoinEvent event) {
        Player player = event.getPlayer();
        plugin.getChallengeManager().updateChallenges(player, false);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onChallengeObjectiveEvent(PlayerChallengeObjectiveEvent event) {
        Player player = event.getPlayer().getPlayer();
        if (player == null) return;

        GeneratedChallenge progress = event.getChallenge();
        String objId = event.getObjective();
        Lang.CHALLENGE_NOTIFY_PROGRESS.getMessage()
            .replace(progress.replacePlaceholders(objId))
            .send(player);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onChallengeCompleteEvent(PlayerChallengeCompleteEvent event) {
        Player player = event.getPlayer().getPlayer();
        if (player == null) return;

        GeneratedChallenge challenge = event.getChallenge();
        ChallengeUser user = event.getUser();
        ChallengeCategory type = challenge.getType();

        challenge.getRewards().forEach(reward -> reward.give(player));

        user.addCompletedChallenge(challenge);

        if (user.isAllChallengesCompleted(type)) {
            type.getCompletionRewards().forEach(reward -> reward.give(player));
        }

        int amount = user.getCompletedChallengesAmount(type);
        type.getMilestones(amount).forEach(milestone -> {
            if (milestone.isRepeatable() || !user.isMilestoneCompleted(type, milestone.getMilestone())) {
                milestone.getCompletionRewards().forEach(reward -> reward.give(player));
            }
        });

        Lang.CHALLENGE_NOTIFY_COMPLETED.getMessage()
            .replace(challenge.replacePlaceholders())
            .send(player);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onChallengeEntitySpawn(CreatureSpawnEvent event) {
        CreatureSpawnEvent.SpawnReason reason = event.getSpawnReason();
        if (Config.OBJECTIVES_ANTI_GLITCH_ENTITY_SPAWN_REASONS.get().contains(reason)) {
            PDCUtil.set(event.getEntity(), Keys.entityTracked, true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onCake(PlayerInteractEvent event) {
        if (event.useInteractedBlock() == Event.Result.DENY) return;

        Block block = event.getClickedBlock();
        if (block == null || block.getType() != Material.CAKE) return;

        Player player = event.getPlayer();
        PlayerItemConsumeEvent consumeEvent = new PlayerItemConsumeEvent(player, new ItemStack(Material.CAKE), EquipmentSlot.HAND);
        this.plugin.getPluginManager().callEvent(consumeEvent);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBrewingClick(InventoryClickEvent event) {
        if (!(event.getInventory() instanceof BrewerInventory inventory)) return;

        BrewingStand stand = inventory.getHolder();
        if (stand == null) return;

        PDCUtil.set(stand, Keys.brewingHolder, event.getWhoClicked().getUniqueId().toString());
        stand.update();
    }
}
