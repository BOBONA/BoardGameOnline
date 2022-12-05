package com.bobona.bgo.service;

import com.bobona.bgo.dao.ConfirmationTokenDao;
import com.bobona.bgo.model.ConfirmationToken;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class ConfirmationTokenService {

    @Autowired
    private ConfirmationTokenDao tokenDao;

    public void saveConfirmationToken(ConfirmationToken token) {
        tokenDao.save(token);
    }

    public void deleteConfirmationToken(Long id) {
        tokenDao.deleteById(id);
    }

    public Optional<ConfirmationToken> findConfirmationTokenWithToken(String token) {
        return tokenDao.findByConfirmationToken(token);
    }

    public Iterable<ConfirmationToken> getTokens() {
        return tokenDao.findAll();
    }
}
