package com.wire.bots.hold.utils;

import com.wire.helium.API;
import com.wire.xenon.WireClient;
import com.wire.xenon.WireClientBase;
import com.wire.xenon.assets.IAsset;
import com.wire.xenon.backend.models.QualifiedId;
import com.wire.xenon.backend.models.User;
import com.wire.xenon.crypto.Crypto;
import com.wire.xenon.models.AssetKey;
import com.wire.xenon.models.otr.PreKey;

import java.util.ArrayList;
import java.util.UUID;

public class HoldWireClient extends WireClientBase implements WireClient {

    private final User user;
    private final QualifiedId conversationId;
    private final String deviceId;

    HoldWireClient(QualifiedId userId, String deviceId, QualifiedId convId, Crypto crypto, API api) {
        super(api, crypto, null);

        User user =  new User();
        user.id = userId;
        this.user = user;

        this.conversationId = convId;
        this.deviceId = deviceId;
    }

    /**
     * <p>
     *     This method used to return the direct UUID (userId) saved in this Class, but userId is now changed to User.
     *     User contains the new QualifiedId, consisting of ID and Domain.
     * </p>
     * <p>
     *     As this method is still used in Xenon, this method needs to return something valid.
     * </p>
     *
     * @return UUID from User Qualified ID
     * @Deprecated Use getSelf() instead
     */
    @Deprecated
    @Override
    public UUID getId() {
        return user.id.id;
    }

    @Override
    public String getDeviceId() {
        return deviceId;
    }

    @Override
    public QualifiedId getConversationId() {
        return conversationId;
    }

    ////////////////////////////////////////////////////////////

    /**
     * <p>
     *     This method returns a User object with minimal data (only containing its QualifiedId) and deprecates the
     *     `getId()` method in this class, as previously by chance both User and Bot/Service only had one type of ID.
     * </p>
     * <p>
     *     Now, Users contain a combination of UUID(id) and String(domain) to compose the User ID, whilst Bot/Service
     *     still uses the same UUID(id) type.
     * </p>
     *
     * @return User with minimal data.
     */
    @Override
    public User getSelf() {
        return user;
    }

    @Override
    public void acceptConnection(QualifiedId user) {

    }

    @Override
    public void uploadPreKeys(ArrayList<PreKey> preKeys) {

    }

    @Override
    public ArrayList<Integer> getAvailablePrekeys() {
        return null;
    }

    @Override
    public byte[] downloadProfilePicture(String assetKey, String domain) {
        return new byte[0];
    }

    @Override
    public AssetKey uploadAsset(IAsset asset) {
        return null;
    }
}
