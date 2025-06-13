package org.example.plugin.concentration.DateBaseMapper;

import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Select;

public interface PlayerScoreMapper {

  @Select("SELECT * FROM player_score_for_concentration ORDER BY score DESC, clear_time ASC")
  List<org.example.plugin.concentration.DateBaseMapper.PlayerScore> selectList();

  @Insert("insert into player_score_for_concentration (player_name,score,clear_time,registered_at) values (#{playerName},#{score},#{clearTime},now())")
  int insert(org.example.plugin.concentration.DateBaseMapper.PlayerScore playerScore);

}

