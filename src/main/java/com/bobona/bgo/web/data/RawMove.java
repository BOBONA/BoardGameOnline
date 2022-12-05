package com.bobona.bgo.web.data;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.Date;
import java.util.List;

public class RawMove {

    private final Date timestamp = new Date();
    private List<Integer> squares;
    private final List<String> inputs;

    public RawMove(List<Integer> squares, List<String> inputs) {
        this.squares = squares;
        this.inputs = inputs;
    }

    public List<Integer> getSquares() {
        return squares;
    }

    public void setSquares(List<Integer> squares) {
        this.squares = squares;
    }

    public List<String> getInputs() {
        return inputs;
    }

    @JsonIgnore
    public Date getTimestamp() {
        return timestamp;
    }
}
