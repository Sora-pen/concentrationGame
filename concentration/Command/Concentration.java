package org.example.plugin.concentration.Command;

import java.io.IOException;
import java.io.InputStream;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
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
import org.example.plugin.concentration.DateBaseMapper.PlayerScore;
import org.example.plugin.concentration.DateBaseMapper.PlayerScoreMapper;
import org.example.plugin.concentration.Main;

/**
 * 神経衰弱ゲームのコマンドクラス。 プレイヤーが額縁を右クリックすることでカードをめくり、ペアを見つけるゲーム。
 * プレイヤーの向いている方向に5x4のグリッド状に額縁を設置し、それぞれにチェストをセットしてカードの裏面として使用する。
 * 10種類のアイテムを2個ずつ用意し、ランダムに配置された額縁の中にペアとして隠す。 最終的なスコアおよび、すべてのペアを発見した際のクリアタイムを競う。
 */
public class Concentration extends CommandBaseProcess implements Listener {

  private final Main main;

  public Concentration(Main main) {
    this.main = main;
  }

  boolean isGaming;
  ArrayList<ItemFrame> spawnedItemFrames;
  ArrayList<Material> itemStacks;
  int selectionStep;
  int gameTime;
  int score;
  int firstChoiceIndex;
  int secondChoiceIndex;

  InputStream inputStream;

  {
    try {
      inputStream = Resources.getResourceAsStream("mybatis-config.xml");
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream);

  @Override
  public boolean onPlayerCommandProcess(Player player, Command command, String s,
      String[] strings) {

    boolean isArgOfListCorrect = strings.length == 1 && strings[0].equals("list");
    if (isArgOfListCorrect) {
      showRanking(player);
      return true;
    }

    if (strings.length != 0) {
      player.sendMessage(
          ChatColor.RED + "引数が不正です。引数なし -> ゲーム開始、引数list -> ランキング確認");
      return false;
    }

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

    isGaming = true;

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

    boolean isItemStackAir = itemFrame.getItem().getType().equals(Material.AIR);
    boolean isNotContainedTheItemFrames = !spawnedItemFrames.contains(itemFrame);
    if (isItemStackAir || isNotContainedTheItemFrames) {
      event.setCancelled(true);
      return;
    }

    int index = spawnedItemFrames.indexOf(itemFrame);
    switch (selectionStep) {
      case 1 -> {
        resetItemFrames();
        spawnedItemFrames.get(index).setItem(new ItemStack(itemStacks.get(index)));
        firstChoiceIndex = index;
        selectionStep = 2;
      }
      case 2 -> {
        boolean isSameChoiceAsFirstChoice = itemFrame.equals(
            spawnedItemFrames.get(firstChoiceIndex));
        if (isSameChoiceAsFirstChoice) {
          event.setCancelled(true);
          return;
        }
        spawnedItemFrames.get(index).setItem(new ItemStack(itemStacks.get(index)));
        secondChoiceIndex = index;
        selectionStep = 1;
        scoringProcess(player);
      }
    }

    if (spawnedItemFrames.stream()
        .allMatch(frame -> frame.getItem().getType().equals(Material.AIR))) {
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
  public void onBlockPlace(BlockPlaceEvent event) {
    if (isGaming) {
      event.setCancelled(true);
    }
  }

  @EventHandler
  public void onHangingBreak(HangingBreakEvent event) {
    if (!isGaming) {
      return;
    }

    if (event.getEntity() instanceof ItemFrame itemFrame) {
      if (spawnedItemFrames.contains(itemFrame)) {
        event.setCancelled(true);
      }
    }
  }

  @EventHandler
  public void onEntityDamage(EntityDamageEvent event) {
    if (!isGaming) {
      return;
    }

    if (event.getEntity() instanceof ItemFrame itemFrame) {
      if (spawnedItemFrames.contains(itemFrame)) {
        event.setCancelled(true);
      }
    }
  }

  /**
   * 神経衰弱ゲームのランキングを1位から3位まで表示する。
   * スコアの高さ(最大100)と、クリアタイムの短さを競う
   * @param player コマンドを実行したプレイヤー
   */
  private void showRanking(Player player) {
    try (SqlSession session = sqlSessionFactory.openSession()) {
      PlayerScoreMapper mapper = session.getMapper(PlayerScoreMapper.class);
      List<PlayerScore> playerScoreList = mapper.selectList();

      player.sendMessage(
          "順位 | プレイヤー名 | スコア | クリアタイム | 日時");
      for (int i = 0; i < Math.min(3, playerScoreList.size()); i++) {
        int playerRanking = i + 1;
        player.sendMessage(
            playerRanking + " | "
                + playerScoreList.get(i).getPlayerName() + " | "
                + playerScoreList.get(i).getScore() + " | "
                + playerScoreList.get(i).getClearTime() + " | "
                + playerScoreList.get(i).getRegisteredAt()
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
      }
    }
  }

  /**
   * 定数の初期化
   */
  private void constantsInitialization() {
    spawnedItemFrames = new ArrayList<>();
    itemStacks = new ArrayList<>();
    gameTime = 60;
    score = 0;
    selectionStep = 1;
    firstChoiceIndex = -1;
    secondChoiceIndex = -1;
  }

  /**
   * プレイヤーの向いている方向を4方向（±X, ±Z）に分類して文字列で返す。
   *
   * @param playerDirectionYaw プレイヤーのヨー角（方向）
   * @return プレイヤーの向きを表す文字列
   */
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
   * @return 設置不可ならtrue、設置可能ならfalse
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
            spawnedItemFrames.add(itemFrame);
          }
          case "directionMinusX" -> {
            itemFrame = ((ItemFrame) world.spawnEntity(location.clone().add(-depth, 0, -width),
                EntityType.ITEM_FRAME));
            itemFrame.setItem(new ItemStack(Material.CHEST));
            itemFrame.setRotation(Rotation.COUNTER_CLOCKWISE);
            spawnedItemFrames.add(itemFrame);
          }
          case "directionPlusZ" -> {
            itemFrame = ((ItemFrame) world.spawnEntity(location.clone().add(width, 0, depth),
                EntityType.ITEM_FRAME));
            itemFrame.setItem(new ItemStack(Material.CHEST));
            itemFrame.setRotation(Rotation.FLIPPED);
            spawnedItemFrames.add(itemFrame);
          }
          case "directionMinusZ" -> {
            itemFrame = ((ItemFrame) world.spawnEntity(location.clone().add(-width, 0, -depth),
                EntityType.ITEM_FRAME));
            itemFrame.setItem(new ItemStack(Material.CHEST));
            itemFrame.setRotation(Rotation.NONE);
            spawnedItemFrames.add(itemFrame);
          }
        }
      }
    }
  }

  /**
   * 10種類のMaterialを2つずつ用意し、合計20個のアイテムをリストに追加してシャッフルする。
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
   * 神経衰弱ゲームの時間を60秒としてカウントダウンを開始し、10秒ごとに残り時間をプレイヤーに通知する。 時間切れまたは全てのペアを見つけた時点でゲームを終了し、結果を表示する。
   *
   * @param player 　コマンドを実行したプレイヤー
   */
  private void gameTimer(Player player) {
    Bukkit.getScheduler().runTaskTimer(main, Runnable -> {
      if (gameTime > 0 && isGaming) {
        gameTime = gameTime - 1;
        if (gameTime % 10 == 0) {
          player.sendMessage("残り" + gameTime + "秒!");
        }
      } else {
        Runnable.cancel();
        isGaming = false;
        spawnedItemFrames.forEach(Entity::remove);
        spawnedItemFrames.clear();
        int clearTime;
        if (gameTime > 0) {
          clearTime = 60 - gameTime;
          player.sendTitle("ゲームクリア!",
              "クリアタイム" + clearTime + "秒", 10, 70, 20);
          } else {
          clearTime = gameTime;
          player.sendTitle("ゲームが終了しました!",
              " 合計 " + score + "点!", 10, 70, 20);
        }
        try (SqlSession session = sqlSessionFactory.openSession(true)) {
          PlayerScoreMapper mapper = session.getMapper(PlayerScoreMapper.class);
          mapper.insert(
              new PlayerScore(player.getName(), score, clearTime));
        }
      }
    }, 0, 20);
  }

  /**
   * 前回選んだ2つがペアではなかった場合、元のチェストに戻す
   */
  private void resetItemFrames() {
    if (firstChoiceIndex == -1 || secondChoiceIndex == -1) {
      return;
    }

    boolean isFirstChoiceAir = spawnedItemFrames.get(firstChoiceIndex).getItem().getType()
        .equals(Material.AIR);
    boolean isSecondChoiceAir = spawnedItemFrames.get(secondChoiceIndex).getItem().getType()
        .equals(Material.AIR);
    if (!isFirstChoiceAir && !isSecondChoiceAir) {
      spawnedItemFrames.get(firstChoiceIndex).setItem(new ItemStack(Material.CHEST));
      spawnedItemFrames.get(secondChoiceIndex).setItem(new ItemStack(Material.CHEST));
    }
  }

  /**
   * プレイヤーが選んだ2つの額縁のアイテムがペアであるかを判定する。
   *
   * @param player 　コマンド実行したプレイヤー
   */
  private void scoringProcess(Player player) {
    if (itemStacks.get(firstChoiceIndex).equals(itemStacks.get(secondChoiceIndex))) {
      score += 10;
      player.sendMessage("当たり!現在" + score + "点!");
      spawnedItemFrames.get(firstChoiceIndex).setItem(new ItemStack(Material.AIR));
      spawnedItemFrames.get(secondChoiceIndex).setItem(new ItemStack(Material.AIR));
    } else {
      player.sendMessage("ハズレ!現在" + score + "点!");
    }
  }

}