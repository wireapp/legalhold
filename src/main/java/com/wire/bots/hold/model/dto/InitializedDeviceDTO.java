package com.wire.bots.hold.model.dto;

import com.wire.xenon.models.otr.PreKey;

import java.util.ArrayList;

public class InitializedDeviceDTO {
    private final ArrayList<PreKey> preKeys;
    private final PreKey lastPreKey;
    private final String fingerprint;

    public InitializedDeviceDTO(ArrayList<PreKey> preKeys, PreKey lastPreKey, String fingerprint) {
        this.preKeys = preKeys;
        this.lastPreKey = lastPreKey;
        this.fingerprint = fingerprint;
    }

    public ArrayList<PreKey> getPreKeys() {
        return preKeys;
    }

    public PreKey getLastPreKey() {
        return lastPreKey;
    }

    public String getFingerprint() {
        return fingerprint;
    }
}
