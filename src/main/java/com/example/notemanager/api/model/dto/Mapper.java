package com.example.notemanager.api.model.dto;

@FunctionalInterface
public interface Mapper<S, T> {
    T map(S source);
}