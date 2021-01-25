package com.wire.bots.hold.utils;

import com.wire.xenon.WireClient;
import com.wire.xenon.WireClientBase;
import com.wire.xenon.assets.IAsset;
import com.wire.xenon.backend.models.Conversation;
import com.wire.xenon.backend.models.User;
import com.wire.xenon.crypto.Crypto;
import com.wire.xenon.models.AssetKey;
import com.wire.xenon.models.otr.PreKey;

import java.util.ArrayList;
import java.util.Collection;
import java.util.UUID;

public class HoldWireClient extends WireClientBase implements WireClient {

    private final UUID userId;
    private final UUID convId;
    private final String deviceId;

    HoldWireClient(UUID userId, String deviceId, UUID convId, Crypto crypto) {
        super(null, crypto, null);
        this.userId = userId;
        this.convId = convId;
        this.deviceId = deviceId;
    }

    @Override
    public UUID getId() {
        return userId;
    }

    @Override
    public String getDeviceId() {
        return deviceId;
    }

    @Override
    public UUID getConversationId() {
        return convId;
    }

    @Override
    public Conversation getConversation() {
        return null;
    }

    ////////////////////////////////////////////////////////////
    @Override
    public byte[] downloadAsset(String assetKey, String assetToken, byte[] sha256Challenge, byte[] otrKey) {
        return new byte[0];
    }

    @Override
    public User getSelf() {
        return null;
    }

    @Override
    public Collection<User> getUsers(Collection<UUID> userIds) {
        return null;
    }

    @Override
    public User getUser(UUID userId) {
        return null;
    }

    @Override
    public void acceptConnection(UUID user) {

    }

    @Override
    public void uploadPreKeys(ArrayList<PreKey> preKeys) {

    }

    @Override
    public ArrayList<Integer> getAvailablePrekeys() {
        return null;
    }

    @Override
    public byte[] downloadProfilePicture(String assetKey) {
        return new byte[0];
    }

    @Override
    public AssetKey uploadAsset(IAsset asset) {
        return null;
    }

}
