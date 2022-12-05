package com.bobona.bgo.dao;

import com.bobona.bgo.model.Game;
import com.bobona.bgo.model.GamePlayer;
import com.bobona.bgo.model.User;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GamePlayerDao extends CrudRepository<com.bobona.bgo.model.GamePlayer, Long> {

    Optional<GamePlayer> findByUserAndGame(User user, Game game);
}
