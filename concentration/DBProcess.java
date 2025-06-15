package org.example.plugin.concentration;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.example.plugin.concentration.DateBaseMapper.PlayerScore;
import org.example.plugin.concentration.DateBaseMapper.PlayerScoreMapper;

/**
 * データベースとの接続、データ取得、登録などの処理を行うクラス
 */
public class DBProcess {

  private SqlSessionFactory sqlSessionFactory;

  public DBProcess() {
    try {
      InputStream inputStream = Resources.getResourceAsStream("mybatis-config.xml");
      this.sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public List<PlayerScore> selectList() {
    try (SqlSession session = sqlSessionFactory.openSession()) {
      PlayerScoreMapper mapper = session.getMapper(PlayerScoreMapper.class);
      return mapper.selectList();
    }
  }

  public void insertToDB(PlayerScore playerScore) {
    try (SqlSession session = sqlSessionFactory.openSession(true)) {
      PlayerScoreMapper mapper = session.getMapper(PlayerScoreMapper.class);
      mapper.insert(playerScore);
    }
  }

}
