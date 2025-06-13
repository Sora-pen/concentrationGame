package org.example.plugin.concentration.DateBaseMapper;

import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * プレイヤーのスコア情報を扱うオブジェクト DBに存在するテーブルと連動する。
 */
@Getter
@Setter
@NoArgsConstructor
public class PlayerScore {

  private int id;
  private String playerName;
  private int score;
  private int clearTime;
  private LocalDateTime registeredAt;

  public PlayerScore(String playerName, int score, int clearTime) {
    this.playerName = playerName;
    this.score = score;
    this.clearTime = clearTime;
  }
}
