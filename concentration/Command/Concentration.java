package org.example.plugin.concentration.Command;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Rotation;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.hanging.HangingBreakEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.example.plugin.concentration.Main;
import org.jetbrains.annotations.NotNull;

public class Concentration extends CommandBaseProcess implements Listener {

  private final Main main;

  public Concentration(Main main) {
    this.main = main;
  }

  boolean isGaming = false;
  ArrayList<ItemFrame> itemFrames;
  ArrayList<Material> itemStacks;
  int selectionStep;
  int gameTime;
  int score;
  int firstChoiceIndex;
  int secondChoiceIndex;

  @Override
  public boolean onPlayerCommandProcess(Player player, Command command, String s,
      String[] strings) {
    isGaming = true;
    constantsInitialization();

    World world = player.getWorld();
    int locationX = player.getLocation().getBlockX();
    int locationY = player.getLocation().getBlockY();
    int locationZ = player.getLocation().getBlockZ();
    float playerDirectionYaw = player.getLocation().getYaw();

    String playerDirection = getPlayerDirection(playerDirectionYaw);

    if (checkPlacementSpace(player, playerDirection, world, locationX, locationY, locationZ)) {
      return false;
    }

    spawnItemFrames(world, locationX, locationY, locationZ, playerDirection);

    makeItemStacksList();

    player.sendMessage("ゲームスタート!額縁を右クリックしてペアを見つけよう!");

    gameTimer(player);

    return true;
  }

  @Override
  public boolean onEtcCommandProcess(CommandSender commandSender, Command command, String s,
      String[] strings) {
    return false;
  }

  @EventHandler
  public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
    Entity entity = event.getRightClicked();
    Player player = event.getPlayer();

    if (!(entity instanceof ItemFrame itemFrame)) {
      return;
    }

    //    空の額縁を選んだときにキャンセル
    if (itemFrame.getItem().getType().equals(Material.AIR)) {
      event.setCancelled(true);
      return;
    }

    if (itemFrames.contains(itemFrame)) {
      int index = itemFrames.indexOf(itemFrame);
      switch (selectionStep) {
        case 1 -> {
          resetItemFrames();
          itemFrames.get(index).setItem(new ItemStack(itemStacks.get(index)));
          firstChoiceIndex = index;
          selectionStep = 2;
        }
        case 2 -> {
          if (itemFrame.equals(itemFrames.get(firstChoiceIndex))) {
            event.setCancelled(true);
            return;
          }
          itemFrames.get(index).setItem(new ItemStack(itemStacks.get(index)));
          secondChoiceIndex = index;
          selectionStep = 1;
          scoringProcess(player);
        }
      }
    }

    if (itemFrames.stream().allMatch(frame -> frame.getItem().getType().equals(Material.AIR))) {
      isGaming = false;
    }

    event.setCancelled(true);
  }

  @EventHandler
  public void onBlockBreak(BlockBreakEvent event) {
    if (isGaming) {
      event.setCancelled(true);
    }
  }

  @EventHandler
  public void onHangingBreak(HangingBreakEvent event) {
    if (isGaming) {
      event.setCancelled(true);
    }
  }

  @EventHandler
  public void onEntityDamage(EntityDamageEvent event) {
    if (isGaming) {
      event.setCancelled(true);
    }
  }

  @EventHandler
  public void onBlockPlace(BlockPlaceEvent event) {
    if (isGaming) {
      event.setCancelled(true);
    }
  }

  /**
   * 定数の初期化
   */
  private void constantsInitialization() {
    itemFrames = new ArrayList<>();
    itemStacks = new ArrayList<>();
    gameTime = 60;
    score = 0;
    selectionStep = 1;
    firstChoiceIndex = -1;
    secondChoiceIndex = -1;
  }

  /**
   * プレイヤーの向いてる方向をX,Z軸に丸めて取得する
   *
   * @param playerDirectionYaw
   * @return プレイヤーの向いてる方向をX, Z軸に丸めて返す
   */
  @NotNull
  private static String getPlayerDirection(float playerDirectionYaw) {
    String playerDirection;
    if (playerDirectionYaw > -135 && playerDirectionYaw <= -45) {
      playerDirection = "directionPlusX";
    } else if (playerDirectionYaw > 45 && playerDirectionYaw <= 135) {
      playerDirection = "directionMinusX";
    } else if (playerDirectionYaw > -45 && playerDirectionYaw <= 45) {
      playerDirection = "directionPlusZ";
    } else {
      playerDirection = "directionMinusZ";
    }
    return playerDirection;
  }

  /**
   * プレイヤー前方の床の幅5ブロック×奥行き4ブロックに額縁を設置可能かを判定する
   *
   * @param player          コマンドを実行したプレイヤー
   * @param playerDirection プレイヤーの向いている方向
   * @param world           コマンドを実行したプレイヤーのいるワールド
   * @param locationX       　プレイヤーが立っている座標X
   * @param locationY       　プレイヤーが立っている座標Y
   * @param locationZ       　プレイヤーが立っている座標Z
   * @return true = NG、false = OK
   */
  private static boolean checkPlacementSpace(Player player, String playerDirection, World world,
      int locationX, int locationY, int locationZ) {
    for (int width = -2; width <= 2; width++) {
      for (int height = -1; height <= 2; height++) {
        for (int depth = 1; depth <= 4; depth++) {
          Material blockType = Material.AIR;
          switch (playerDirection) {
            case "directionPlusX" ->
                blockType = world.getBlockAt(locationX + depth, locationY + height,
                    locationZ + width).getType();
            case "directionMinusX" ->
                blockType = world.getBlockAt(locationX - depth, locationY + height,
                    locationZ - width).getType();
            case "directionPlusZ" ->
                blockType = world.getBlockAt(locationX + width, locationY + height,

                    locationZ + depth).getType();
            case "directionMinusZ" ->
                blockType = world.getBlockAt(locationX - width, locationY + height,
                    locationZ - depth).getType();
          }
          if (height != -1 && blockType != Material.AIR) {
            player.sendMessage(
                "前方の横5*奥行き4ブロックに空きスペースが必要です。移動するか、ブロックを取り除いてください。");
            return true;
          } else if (height == -1 && !blockType.isSolid()) {
            player.sendMessage(
                "前方の横5*奥行き4ブロックの床にブロックがが必要です。移動するか、ブロックを設置してください。");
            return true;
          }
        }
      }
    }
    return false;
  }

  /**
   * プレイヤー前方の床の幅5ブロック×奥行き4ブロックに額縁を設置し、チェストをセットする。
   *
   * @param world           プレイヤーがコマンドを実行したワールド
   * @param locationX       プレイヤーが立っている座標X
   * @param locationY       プレイヤーが立っている座標Y
   * @param locationZ       プレイヤーが立っている座標Z
   * @param playerDirection 　プレイヤーの向いている方向
   */
  private void spawnItemFrames(World world, int locationX, int locationY, int locationZ,
      String playerDirection) {
    Location location = new Location(world, locationX, locationY, locationZ);
    for (int width = -2; width <= 2; width++) {
      for (int depth = 1; depth <= 4; depth++) {
        ItemFrame itemFrame;
        switch (playerDirection) {
          case "directionPlusX" -> {
            itemFrame = ((ItemFrame) world.spawnEntity(location.clone().add(depth, 0, width),
                EntityType.ITEM_FRAME));
            itemFrame.setItem(new ItemStack(Material.CHEST));
            itemFrame.setRotation(Rotation.CLOCKWISE);
            itemFrames.add(itemFrame);
          }
          case "directionMinusX" -> {
            itemFrame = ((ItemFrame) world.spawnEntity(location.clone().add(-depth, 0, -width),
                EntityType.ITEM_FRAME));
            itemFrame.setItem(new ItemStack(Material.CHEST));
            itemFrame.setRotation(Rotation.COUNTER_CLOCKWISE);
            itemFrames.add(itemFrame);
          }
          case "directionPlusZ" -> {
            itemFrame = ((ItemFrame) world.spawnEntity(location.clone().add(width, 0, depth),
                EntityType.ITEM_FRAME));
            itemFrame.setItem(new ItemStack(Material.CHEST));
            itemFrame.setRotation(Rotation.FLIPPED);
            itemFrames.add(itemFrame);
          }
          case "directionMinusZ" -> {
            itemFrame = ((ItemFrame) world.spawnEntity(location.clone().add(-width, 0, -depth),
                EntityType.ITEM_FRAME));
            itemFrame.setItem(new ItemStack(Material.CHEST));
            itemFrame.setRotation(Rotation.NONE);
            itemFrames.add(itemFrame);
          }
        }
      }
    }
  }

  /**
   * ペアとなるエンティティを10組作成しリストの中に入れ、中身をシャッフルする。
   */
  private void makeItemStacksList() {
    List<Material> materials = List.of(
        Material.GOLDEN_AXE, Material.DIAMOND, Material.APPLE, Material.BOOK,
        Material.BREAD, Material.BONE, Material.COAL, Material.EMERALD,
        Material.ENDER_PEARL, Material.GOLD_INGOT);

    for (Material material : materials) {
      itemStacks.add(material);
      itemStacks.add(material);
    }

    Collections.shuffle(itemStacks);
  }

  /**
   * 神経衰弱ゲームのタイマー
   *
   * @param player
   */
  private void gameTimer(Player player) {
    Bukkit.getScheduler().runTaskTimer(main, Runnable -> {
      if (gameTime >= 0 && isGaming) {
        gameTime = gameTime - 1;
        if (gameTime % 10 == 0) {
          player.sendMessage("残り" + gameTime + "秒!");
        }
      } else {
        Runnable.cancel();
        isGaming = false;
        itemFrames.forEach(Entity::remove);
        itemFrames.clear();
        if (gameTime > 0) {
          player.sendTitle("ゲームクリア!",
              "クリアタイム" + (60 - gameTime) + "秒", 10, 70, 20);
        } else {
          player.sendTitle("ゲームが終了しました!",
              " 合計 " + score + "点!", 10, 70, 20);
        }
      }
    }, 0, 20);
  }

  /**
   * 前回選んだ2つがペアではなかった場合、元のチェストに戻す
   */
  private void resetItemFrames() {
    if (selectionStep == 2
        && !itemFrames.get(firstChoiceIndex).getItem().getType().equals(Material.AIR)
        && !itemFrames.get(secondChoiceIndex).getItem().getType().equals(Material.AIR)) {
      itemFrames.get(firstChoiceIndex).setItem(new ItemStack(Material.CHEST));
      itemFrames.get(secondChoiceIndex).setItem(new ItemStack(Material.CHEST));
    }
  }

  /**
   * プレイヤーが選んだ2つの額縁のアイテムがペアであるかを判定する。
   *
   * @param player
   */
  private void scoringProcess(Player player) {
    if (itemStacks.get(firstChoiceIndex).equals(itemStacks.get(secondChoiceIndex))) {
      score += 10;
      player.sendMessage("当たり!現在" + score + "点!");
      itemFrames.get(firstChoiceIndex).setItem(new ItemStack(Material.AIR));
      itemFrames.get(secondChoiceIndex).setItem(new ItemStack(Material.AIR));
    } else {
      player.sendMessage("ハズレ!現在" + score + "点!");
    }
  }

}