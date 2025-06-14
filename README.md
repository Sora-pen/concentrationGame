
# ミニゲーム：神経衰弱
  Java版マインクラフトの神経衰弱ゲームを実装したSpigotサーバー用のプラグイン。 

## はじめに
  Java学習のアウトプットとして、Minecraft（Java版）上で遊べる「神経衰弱」ミニゲームのプラグインを作成しました。<br>
  **Spigot API** と **MySQL** を使用し、スコア記録・ランキング表示機能付きの本格的なゲーム体験を実現しています。

## 工夫した点

  ・**直感的なゲーム体験**： 額縁の裏面にチェスト、表面にアイテムを表示し、分かりやすさを重視

  ・**向きに応じた盤面生成**： プレイヤーの視線方向に応じて盤面（5×4の額縁）が出現

  ・**ランキング表示機能**： MySQLに記録されたスコアから上位3名を表示するランキングを実装

  ・**可読性を意識したコード**： 他人が読んでも意図が伝わるよう丁寧にコーディング

## プレイ動画



## ゲームの概要
  プレイヤーが額縁を右クリックすることでカードをめくり、ペアを見つけるミニゲームです。
  プレイヤーの向いている方向に5x4のグリッド状にチェストがセットされた額縁を設置し、そこに10種類のアイテムを2個ずつ用意し、ランダムに配置された額縁の中にペアとして隠す。
  最終的なスコアと、すべてのペアを発見した際のクリアタイムを競う。


## データベース設計
|属性　　　　 |設定値　　　 |
|-----|-----|
| データベース名 | spigot_server |
| テーブル名 | player_score | 

## データベース構成
|カラム名　　　　 |説明　　 |
|-----|-----|
| id | 主キー、自動採番 |
| player_name | プレイヤー名 |
| score | プレイヤーが獲得したポイント|
| clear_time | クリアした時間（秒）（タイムアップ時間） | 
| registered_at | 登録日時 | 

## 遊び方

１．コマンド（　/concentration　）を入力してください

２. プレイヤー前方5×4ブロックにチェストが入った額縁が出現します。<br>　　額縁の中には、2つずつペアになったアイテム（全10種）がランダムに設定されています

３．任意の額縁を右クリックすると、その中に設定されたアイテムが表示されます

４．次に、もう1つの額縁を右クリックして、同じアイテムを探してください

５．2つのアイテムがペアの場合： 10点を獲得し、ペアの額縁のアイテムが消滅します<br>　　アイテムが異なる場合： 得点は入らず、次の額縁を右クリックしたタイミングでチェストの表示に戻ります

６. この操作を**制限時間60秒**の間、繰り返してください

７．最終的なスコアおよび、すべてのペアを発見した際のクリアタイムを競います

８. コマンド（　/concentration list　）を入力することでランキング上位３位まで表示されます

## スコア確認動画

## 対応バージョン

  ・Minecraft : 1.21.5

  ・Spigot : 1.21.5
