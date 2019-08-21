package com.wire.bots.hold.utils;

import com.wire.bots.sdk.WireClient;
import com.wire.bots.sdk.WireClientBase;
import com.wire.bots.sdk.assets.IAsset;
import com.wire.bots.sdk.assets.IGeneric;
import com.wire.bots.sdk.crypto.Crypto;
import com.wire.bots.sdk.models.AssetKey;
import com.wire.bots.sdk.models.otr.PreKey;
import com.wire.bots.sdk.server.model.Conversation;
import com.wire.bots.sdk.server.model.User;

import java.io.File;
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
    public UUID sendText(String txt) {
        return null;
    }

    @Override
    public UUID sendDirectText(String txt, UUID userId) {
        return null;
    }

    @Override
    public UUID sendText(String txt, long expires) {
        return null;
    }

    @Override
    public UUID sendText(String txt, UUID mention) {
        return null;
    }

    @Override
    public UUID sendLinkPreview(String url, String title, IGeneric image) {
        return null;
    }

    @Override
    public UUID sendDirectLinkPreview(String url, String title, IGeneric image, UUID userId) {
        return null;
    }

    @Override
    public UUID sendPicture(byte[] bytes, String mimeType) {
        return null;
    }

    @Override
    public UUID sendDirectPicture(byte[] bytes, String mimeType, UUID userId) {
        return null;
    }

    @Override
    public UUID sendPicture(IGeneric image) {
        return null;
    }

    @Override
    public UUID sendDirectPicture(IGeneric image, UUID userId) {
        return null;
    }

    @Override
    public UUID sendAudio(byte[] bytes, String name, String mimeType, long duration) {
        return null;
    }

    @Override
    public UUID sendVideo(byte[] bytes, String name, String mimeType, long duration, int h, int w) {
        return null;
    }

    @Override
    public UUID sendFile(File file, String mime) {
        return null;
    }

    @Override
    public UUID sendDirectFile(File file, String mime, UUID userId) {
        return null;
    }

    @Override
    public UUID sendDirectFile(IGeneric preview, IGeneric asset, UUID userId) {
        return null;
    }

    @Override
    public UUID ping() {
        return null;
    }

    @Override
    public UUID sendReaction(UUID msgId, String emoji) {
        return null;
    }

    @Override
    public UUID deleteMessage(UUID msgId) {
        return null;
    }

    @Override
    public UUID editMessage(UUID replacingMessageId, String text) {
        return null;
    }

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

    @Override
    public void call(String content) {

    }
}
