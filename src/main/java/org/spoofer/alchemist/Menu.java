package org.spoofer.alchemist;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.permission.Permission;
import org.api.spoofer.slibandapi.inventory.InventoryBuilder;
import org.api.spoofer.slibandapi.mutation.sub.*;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;
import org.bukkit.scheduler.BukkitRunnable;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.stream.Stream;

public final class Menu {


    public static final Set<Player> isActive = new HashSet<>();


    @Start
    public void start(final @Config("menu.name") String name,
                      final @Config("menu.size") int size,
                      final @Config(value = "item", section = {"name", "material", "head" , "slot" , "lore"} , ignore = true) Set<ConfigMapper> mappers,
                      final @Command("alchemist") CommandBase base,
                      final @Qulifair(value = "Economy" , def = true) Economy economy,
                      final @Qulifair(value = "Permission" , def = true) Permission permission,
                      final @Config("menu.action") int action,
                      final @Config(value = "buster", section = {"chance", "discount"}) Set<ConfigMapper> buster,
                      final @Config("message.lose") String lose,
                      final @Config("message.win") String win,
                      final @Config("message.no_money") String no_money,
                      final @Config("message.no_potion") String no_potion,
                      final @Config("limit_potion") int potion_max,
                      final @Config("message.limit") String message_limits,
                      final @Config(value = "animation.material" , ignore = true) String material_animation,
                      final @Config("animation.name") String name_animation,
                      final @Config(value = "animation.slot" , ignore = true , isColor = false) List<String> slot_animation,
                      final @Config("animation.potion") int slot_potion,
                      final @Config("animation.time") int time_potion,
                      final @Config("animation.bad_material") String material_bad,
                      final @Config("animation.name_bad") String name_bad,
                      final @Config("animation.close_inventory") int close,
                      final @Config("message.potion_fuck") String potion_fuck,
                      final @Config("potion_name") String potion_name
    ) {

        final Stream<PotionEffectType> NegativePotions = Stream.of(PotionType.INSTANT_DAMAGE.getEffectType(), PotionType.POISON.getEffectType(), PotionType.SLOWNESS.getEffectType(), PotionType.WEAKNESS.getEffectType(), PotionType.SLOW_FALLING.getEffectType());

        final Stream<PotionEffectType> Irreg = Stream.of(PotionType.REGEN.getEffectType(), PotionType.LUCK.getEffectType(), PotionType.POISON.getEffectType(), PotionType.TURTLE_MASTER.getEffectType(), PotionType.INSTANT_DAMAGE.getEffectType(), PotionType.INSTANT_HEAL.getEffectType());

        final Map<String, EntryHash> chance = new HashMap<>();

        for (final ConfigMapper b : buster) {
            chance.put(b.getName(), new EntryHash(b.get("chance"), b.get("discount")));
        }

        base.setTabCompleter((commandSender, command, s, strings) -> Collections.emptyList());

        final ItemStack itemBad = new ItemStack(Material.getMaterial(material_bad));
        final ItemMeta itemBadMeta = itemBad.getItemMeta();
        itemBadMeta.setDisplayName(name_bad);
        itemBad.setItemMeta(itemBadMeta);

        final ItemStack animated = new ItemStack(Material.getMaterial(material_animation));
        final ItemMeta metaan = animated.getItemMeta();
        metaan.setDisplayName(name_animation);
        animated.setItemMeta(metaan);

        final Consumer<Player> playerConsumer = player -> {


            final InventoryBuilder builder = new InventoryBuilder(name, size);

            builder.setEventOpen(inventoryOpenEvent -> isActive.add(player));

            final EntryHash hash = chance.get(permission.getPrimaryGroup(player));

            for (final ConfigMapper m : mappers) {

                ItemStack itemStack = new ItemStack(Objects.requireNonNull(Material.getMaterial(m.get("material")), "Material is null"));

                final ItemMeta itemMeta = itemStack.getItemMeta();

                String displayname = m.get("name");

                if (displayname != null) {
                    itemMeta.setDisplayName(m.get("name"));
                }
                final String lore = m.get("lore");

                if (lore != null) {
                    itemMeta.setLore(Arrays.asList(lore.replace("%chance%", String.valueOf(hash.getInteger1())).replace("%discount%", String.valueOf(hash.getInteger2())).replace("%balance%" , String.valueOf(economy.getBalance(player))).split("\n")));
                }
                final String head = m.get("head");

                if (head != null) {
                    itemStack = new ItemStack(Material.PLAYER_HEAD);

                    if (Bukkit.getBukkitVersion().contains("1.12")) {
                        itemStack = new ItemStack(Material.valueOf("HEAD"));
                    }

                    final GameProfile profile = new GameProfile(UUID.randomUUID(), null);

                    profile.getProperties().put("textures", new Property("textures", head));

                    try {
                        Field profileField = itemMeta.getClass().getDeclaredField("profile");
                        profileField.setAccessible(true);
                        profileField.set(itemMeta, profile);
                    } catch (Exception ignore) {}
                }

                itemStack.setItemMeta(itemMeta);


                final List<Integer> integers = m.get("slot");

                if (integers != null && !integers.isEmpty()) {
                    for (Integer i : integers) {
                        builder.addItem(i, itemStack);
                    }
                }
            }

            builder.setEventClose(event -> isActive.remove(player));


            final AtomicBoolean aBoolean = new AtomicBoolean(true);

            builder.setEventClose(e -> {
                final ItemStack[] items = builder.getInventory().getContents();
                for (int i = 0; i < items.length; i++) {
                    final ItemStack stack = items[i];
                    if(stack != null && !builder.getAdder().contains(i)) {
                        if (player.getInventory().firstEmpty() == -1) {
                            player.getWorld().dropItem(player.getLocation() , stack);
                        } else {
                            player.getInventory().addItem(stack);
                        }
                    }
                }
                InventoryBuilder.clear(builder.getInventory());
            });

            builder.setEventClick(event -> {
                if(builder.getAdder().contains(event.getSlot())) {
                    event.setCancelled(true);
                    if (event.getSlot() == action && aBoolean.get()) {
                        if (economy.getBalance(player) >= hash.getInteger2()) {
                            final Set<PotionMeta> list = new HashSet<>();

                            final ItemStack[] content = event.getInventory().getContents();

                            for (ItemStack item : content) {
                                if (item != null && item.getType() == Material.POTION) {
                                    list.add((PotionMeta) item.getItemMeta());

                                }
                            }

                                if(list.size() <= potion_max) {
                                    if (list.size() >= 2) {
                                        builder.clearAll();

                                        new BukkitRunnable() {
                                            @Override
                                            public void run() {
                                                aBoolean.set(true);
                                                for (String an : slot_animation) {
                                                    final String[] split = an.split(";");
                                                    builder.addItem(Integer.parseInt(split[0]), animated);
                                                    builder.addItem(Integer.parseInt(split[1]), animated);
                                                    player.updateInventory();
                                                    try {
                                                        Thread.sleep(time_potion);
                                                    } catch (InterruptedException e) {
                                                        throw new RuntimeException(e);
                                                    }
                                                }
                                                if (100 - ThreadLocalRandom.current().nextInt(0, 100) < hash.getInteger1()) {

                                                    economy.withdrawPlayer(player, hash.getInteger2());

                                                    final ItemStack potion = new ItemStack(Material.POTION);

                                                    final PotionMeta meta = (PotionMeta) potion.getItemMeta();

                                                    meta.setColor(Color.fromRGB(ThreadLocalRandom.current().nextInt(0, 255)));

                                                    for (final PotionMeta effect : list) {


                                                        for(final PotionEffect eff : effect.getCustomEffects()) {
                                                            meta.addCustomEffect(eff, true);
                                                        }

                                                        final PotionType pt = effect.getBasePotionData().getType();


                                                        if (pt != null) {
                                                            final PotionEffectType pet = pt.getEffectType();


                                                            if (pet != null) {

                                                                final boolean extended = effect.getBasePotionData().isExtended();

                                                                final boolean upgraded = effect.getBasePotionData().isUpgraded();

                                                                final boolean irregular = NegativePotions.anyMatch(s -> s.equals(pet));

                                                                final boolean negative = Irreg.anyMatch(s -> s.equals(pet));

                                                                if (!extended && !upgraded && !irregular) {
                                                                    meta.addCustomEffect(negative ? new PotionEffect(pet, (int) (1200 * 1.5), 0) : new PotionEffect(pet, 1200 * 3, 0), true);
                                                                } else if (!extended && upgraded && !irregular) {
                                                                    meta.addCustomEffect(negative ? new PotionEffect(pet, 400, 3) : new PotionEffect(pet, (int) (1200 * 1.5D), 1), true);
                                                                } else if (extended && !upgraded && !irregular) {
                                                                    meta.addCustomEffect(negative ? new PotionEffect(pet, 1200 * 4, 0) : new PotionEffect(pet, 1200 * 8, 0), true);
                                                                } else if (pt.equals(PotionType.REGEN) || pt.equals(PotionType.POISON)) {
                                                                    meta.addCustomEffect(extended ? new PotionEffect(pet, (int) (1200 * 1.5), 0) : upgraded ? negative ? new PotionEffect(pet, (int) (21.6 * 20), 1) : new PotionEffect(pet, 22 * 20, 1) : new PotionEffect(pet, 45 * 20, 0), true);
                                                                } else if (pt.equals(PotionType.INSTANT_DAMAGE) || pt.equals(PotionType.INSTANT_HEAL)) {
                                                                    meta.addCustomEffect(upgraded ? new PotionEffect(pet, 1, 1) : new PotionEffect(pet, 1, 0), true);
                                                                } else if (pt.equals(PotionType.LUCK)) {
                                                                    meta.addCustomEffect(new PotionEffect(pet, 5 * 1200, 0), true);
                                                                }
                                                            }
                                                        }
                                                    }
                                                    meta.setDisplayName(potion_name);
                                                    potion.setItemMeta(meta);

                                                    builder.getInventory().setItem(slot_potion , potion);

                                                    player.sendMessage(win);


                                                }else {
                                                    builder.addItem(slot_potion , itemBad);
                                                    player.sendMessage(lose);
                                                }


                                                Bukkit.getScheduler().runTaskLater(Main.getInstance() , player::closeInventory, close * 20L);
                                            }
                                        }.runTask(Main.getInstance());
                                }else {
                                    player.sendMessage(no_potion);
                                }
                            }else {
                                    player.sendMessage(message_limits);
                                }
                        } else {
                            player.sendMessage(no_money);
                        }
                    }
                }
            });

            player.openInventory(builder.getInventory());

        };

        base.setCommandExecutor((commandSender, command, s, strings) -> {
            if (commandSender instanceof Player) {
                if (commandSender.hasPermission("OccupantAlchemist.*")) {
                    playerConsumer.accept(((Player) commandSender));
                }
            }
            return true;
        });

        base.register();
    }


}
