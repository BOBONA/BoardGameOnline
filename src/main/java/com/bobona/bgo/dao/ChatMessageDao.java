package com.bobona.bgo.dao;

import com.bobona.bgo.model.ChatMessage;
import org.springframework.data.repository.CrudRepository;

public interface ChatMessageDao extends CrudRepository<ChatMessage, Long> {
}
