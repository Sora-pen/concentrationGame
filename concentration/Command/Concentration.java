package org.example.plugin.concentration.Command;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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

  public Concentration(Main main) {
  }

  ArrayList<ItemFrame> itemFrames = new ArrayList<>();
  ArrayList<Material> itemStacks = new ArrayList<>();
  int stepNumber = 0;
  int score;
  int firstChoiceIndex = -1;
  int secondChoiceIndex = -1;
  boolean isGaming =false;

  @Override
  public boolean onPlayerCommandProcess(Player player, Command command, String s, String[] strings)
      throws IOException {
    isGaming=true;
    score=0;
    stepNumber=0;
    itemFrames = new ArrayList<>();

    World world = player.getWorld();
    int locationX = player.getLocation().getBlockX();
    int locationY = player.getLocation().getBlockY();
    int locationZ = player.getLocation().getBlockZ();

    String playerDirection = getPlayerDirection(player);

    if (checkPlacementSpace(player, playerDirection, world, locationX, locationY, locationZ)) {
      return false;
    }

    spawnItemFrames(world, locationX, locationY, locationZ, playerDirection);

    makeItemStacksList();

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

    if (!(entity instanceof ItemFrame)) return;

    ItemFrame itemFrame = (ItemFrame) entity;

//    額縁に対するアイテムのセットを制限
    if (itemFrame.getItem().getType().equals(Material.AIR)) {
      event.setCancelled(true);
      return;
    }

    if (itemFrames.contains(itemFrame)) {
      int index = itemFrames.indexOf(itemFrame);
      if (stepNumber % 2 == 0) {
        firstChoiceProcess(index);
      } else if (stepNumber % 2 == 1){
        if (secondChoiceProcess(event, itemFrame, index, player)) return;
      }
    }

    finishGame(player);

    event.setCancelled(true);
  }

  @EventHandler
  public void onBlockBreak(BlockBreakEvent event){
    if(isGaming){
      event.setCancelled(true);
    }
  }

  @EventHandler
  public void onHangingBreak(HangingBreakEvent event){
    if(isGaming){
      event.setCancelled(true);
    }
  }

  @EventHandler
  public void onEntityDamage(EntityDamageEvent event){
    if (isGaming) {
      event.setCancelled(true);
    }
  }

  @EventHandler
  public void onBlockPlace(BlockPlaceEvent event){
    if (isGaming) {
      event.setCancelled(true);
    }
  }

  /**
   * コマンドを実行したプレイヤーの向いている方向をX軸とZ軸に丸めて取得する。
   *
   * @param player コマンドを実行したプレイヤー
   * @return プレイヤーの向いている方向に対応した文字列
   */
  @NotNull
  private static String getPlayerDirection(Player player) {
    float playerDirectionYaw = player.getLocation().getYaw();
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
   * プレイヤーが1つ目の額縁を選択したときに行う処理
   *
   * @param index
   */
  private void firstChoiceProcess(int index) {
    resetItemFrames();
    itemFrames.get(index).setItem(new ItemStack(itemStacks.get(index)));
    firstChoiceIndex = index;
    stepNumber += 1;
  }

  /**
   * ペアが合わず残った場合、次の組の最初を選ぶときにチェストに戻す。
   */
  private void resetItemFrames() {
    if (stepNumber >= 2
        && !itemFrames.get(firstChoiceIndex).getItem().getType().equals(Material.AIR)
        && !itemFrames.get(secondChoiceIndex).getItem().getType().equals(Material.AIR)) {
      itemFrames.get(firstChoiceIndex).setItem(new ItemStack(Material.CHEST));
      itemFrames.get(secondChoiceIndex).setItem(new ItemStack(Material.CHEST));
    }
  }

  /**
   * プレイヤーが2つ目の額縁を選択したときに行う処理
   * @param event
   * @param itemFrame
   * @param index
   * @param player
   * @return
   */
  private boolean secondChoiceProcess(PlayerInteractEntityEvent event, ItemFrame itemFrame, int index,
      Player player) {
    if(itemFrame.equals(itemFrames.get(firstChoiceIndex))){
      event.setCancelled(true);
      return true;
    }
    itemFrames.get(index).setItem(new ItemStack(itemStacks.get(index)));
    secondChoiceIndex = index;
    stepNumber += 1;
    scoreingProcess(player);
    return false;
  }

  /**
   * プレイヤーが選んだ2つの額縁のアイテムがペアであるかを判定する。
   * @param player
   */
  private void scoreingProcess(Player player) {
    if (itemStacks.get(firstChoiceIndex).equals(itemStacks.get(secondChoiceIndex))) {
      score += 1;
      player.sendMessage("当たり!現在の手数" + score + "手");
      itemFrames.get(firstChoiceIndex).setItem(new ItemStack(Material.AIR));
      itemFrames.get(secondChoiceIndex).setItem(new ItemStack(Material.AIR));
    } else {
      score += 1;
      player.sendMessage("ハズレ!現在" + score + "手");
    }
  }

  /**
   * ペアがすべてなくなったときにゲームを終了する
   * @param player
   */
  private void finishGame(Player player) {
    if (itemFrames.stream().allMatch(frame -> frame.getItem().getType().equals(Material.AIR))) {
      itemFrames.forEach(ItemFrame::remove);
      itemFrames.clear();
      player.sendTitle("ゲームが終了しました!",
          player.getName() + " 合計 " + score + "手!", 20, 5 * 20, 20);
      isGaming=false;
    }
  }

}