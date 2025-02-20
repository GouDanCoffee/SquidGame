package jossc.squidgame.phase;

import cn.nukkit.Player;
import cn.nukkit.block.Block;
import cn.nukkit.block.BlockAir;
import cn.nukkit.entity.Entity;
import cn.nukkit.entity.projectile.EntityArrow;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.EventPriority;
import cn.nukkit.event.entity.EntityDamageEvent;
import cn.nukkit.event.entity.ProjectileHitEvent;
import cn.nukkit.item.Item;
import cn.nukkit.item.ItemArrow;
import cn.nukkit.item.ItemID;
import cn.nukkit.item.enchantment.Enchantment;
import cn.nukkit.level.MovingObjectPosition;
import cn.nukkit.level.Position;
import cn.nukkit.math.Vector3;
import cn.nukkit.utils.Config;
import cn.nukkit.utils.ConfigSection;
import java.time.Duration;
import java.util.LinkedList;
import java.util.List;
import net.josscoder.gameapi.customitem.CustomItem;
import net.josscoder.gameapi.map.GameMap;
import net.josscoder.gameapi.user.User;
import net.josscoder.gameapi.user.storage.LocalStorage;
import net.josscoder.gameapi.util.BlockUtils;
import net.josscoder.gameapi.util.VectorUtils;

public class SugarHoneycombs extends Microgame {

  private CustomItem burningNeedle;
  private boolean canReciveDamage = false;

  public SugarHoneycombs(Duration duration) {
    super(duration);
    registerBurningNeedleItem();
  }

  private void registerBurningNeedleItem() {
    burningNeedle =
      new CustomItem(Item.get(ItemID.BOW), "&r&6&lBurning Needle");
    burningNeedle.addEnchantment(
      Enchantment.getEnchantment(Enchantment.ID_BOW_INFINITY)
    );
    burningNeedle.setTransferable(false);
  }

  @Override
  public String getName() {
    return "Sugar Honeycombs";
  }

  @Override
  public String getInstruction() {
    return "Break 10 blocks with the Burning Needle to win!";
  }

  @Override
  public void setupMap(Config config) {
    ConfigSection section = config.getSection("maps.sugarHoneycombsMap");

    map =
      new GameMap(
        game,
        section.getString("name"),
        VectorUtils.stringToVector(section.getString("safeSpawn"))
      );

    List<Vector3> spawns = new LinkedList<>();

    for (int i = 1; i <= game.getMaxPlayers(); i++) {
      spawns.add(VectorUtils.stringToVector(section.getString("spawns." + i)));
    }

    map.setSpawns(GameMap.SOLO, spawns);
    game.getGameMapManager().addMap(map);
  }

  @Override
  public List<String> getScoreboardLines(User user) {
    List<String> lines = super.getScoreboardLines(user);

    int blocksBroken = user.getLocalStorage().getInteger("blocks_broken");

    lines.add("\uE19D Blocks Broken " + blocksBroken + "/10");

    return lines;
  }

  @Override
  public void onGameStart() {
    giveAttributes();
    canReciveDamage = true;
  }

  private void giveAttributes() {
    getNeutralPlayers()
      .forEach(
        player -> {
          player.setImmobile();
          player.getInventory().setItem(0, burningNeedle.build());
          player.getInventory().setItem(9, new ItemArrow());

          User user = userFactory.get(player);

          if (user != null) {
            user.updateInventory();
          }
        }
      );
  }

  @Override
  public void onGameUpdate() {}

  @EventHandler(priority = EventPriority.HIGHEST)
  public void onHit(ProjectileHitEvent event) {
    Entity entity = event.getEntity();

    if (!(entity instanceof EntityArrow)) {
      return;
    }

    Entity shootingEntity = ((EntityArrow) entity).shootingEntity;

    if (!(shootingEntity instanceof Player)) {
      return;
    }

    Player player = (Player) shootingEntity;

    if (!(entity.isCollided && ((EntityArrow) entity).hadCollision)) {
      return;
    }

    entity.close();

    MovingObjectPosition position = event.getMovingObjectPosition();

    List<Block> blocksAround = BlockUtils.getNearbyBlocks(
      new Position(
        position.blockX,
        position.blockY,
        position.blockZ,
        player.getLevel()
      )
        .floor(),
      1
    );

    for (Block block : blocksAround) {
      if (block == null || block.getId() == Item.AIR) {
        continue;
      }

      if (
        block.getId() == Block.STAINED_HARDENED_CLAY ||
        block.getId() == Block.CLAY_BLOCK
      ) {
        User user = userFactory.get(player);

        if (user != null) {
          player.getLevel().setBlock(block, new BlockAir(), false, true);

          user.playSound("random.levelup", 2, 3);

          LocalStorage storage = user.getLocalStorage();

          storage.set("blocks_broken", storage.getInteger("blocks_broken") + 1);

          user.sendMessage("&l&a» +1 block broken");

          if (storage.getInteger("blocks_broken") >= 10) {
            win(player);
          }
        }

        break;
      }

      applyDamage(player);
      break;
    }
  }

  private void applyDamage(Player player) {
    player.attack(4.0f);
  }

  @Override
  @EventHandler(priority = EventPriority.NORMAL)
  public void onDamage(EntityDamageEvent event) {
    if (
      !canReciveDamage ||
      !event.getCause().equals(EntityDamageEvent.DamageCause.CUSTOM)
    ) {
      super.onDamage(event);

      return;
    }

    Entity entity = event.getEntity();

    if (!(entity instanceof Player)) {
      return;
    }

    Player player = (Player) entity;

    User user = userFactory.get(player);

    if (user == null) {
      return;
    }

    if (event.getFinalDamage() < entity.getHealth()) {
      return;
    }

    event.setCancelled();

    lose(player, true);
  }

  @Override
  public void onGameEnd() {
    canReciveDamage = false;
  }
}
