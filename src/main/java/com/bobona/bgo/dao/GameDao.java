package com.bobona.bgo.dao;

import com.bobona.bgo.model.Game;
import com.bobona.bgo.model.User;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface GameDao extends CrudRepository<Game, Long> {

    @Query("select p.user from GamePlayer p where p.game = ?1 and p.team = ?2")
    List<User> getTeam(Game game, int teamId);

    @Query("select g from Game g left join fetch g.gamePlayers where g.id = ?1")
    Optional<Game> findById(Long id);

    @Query("select g from Game g left join fetch g.gamePlayers")
    Set<Game> findAll();
}
