package org.example.plugin.concentration.Command;

import java.io.IOException;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public abstract class CommandBaseProcess implements CommandExecutor {

  @Override
  public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command,
      @NotNull String s, @NotNull String[] strings) {
    if (commandSender instanceof Player player) {
      try {
        return onPlayerCommandProcess(player, command, s, strings);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    } else {
      return onEtcCommandProcess(commandSender, command, s, strings);
    }
  }

  /**
   * プレイヤーがコマンド使った際に実行する
   *
   * @param player  コマンド実行したプレイヤー
   * @param command コマンド
   * @param s       ラベル
   * @param strings コマンド引数
   * @return 処理の実行有無
   */
  public abstract boolean onPlayerCommandProcess(Player player, Command command,
      String s, String[] strings) throws IOException;

  /**
   * プレイヤー以外がコマンドを使った際に実行する
   *
   * @param commandSender 　コマンド実行した対象
   * @param command       コマンド
   * @param s             ラベル
   * @param strings       コマンド引数
   * @return 処理の実行有無
   */
  public abstract boolean onEtcCommandProcess(CommandSender commandSender, Command command,
      String s, String[] strings);

}
