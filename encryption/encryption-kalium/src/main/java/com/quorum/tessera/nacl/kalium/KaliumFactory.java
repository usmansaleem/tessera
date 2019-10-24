package com.quorum.tessera.nacl.kalium;

import com.quorum.tessera.encryption.Encryptor;
import com.quorum.tessera.encryption.EncryptorFactory;
import org.abstractj.kalium.NaCl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KaliumFactory implements EncryptorFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(KaliumFactory.class);

    @Override
    public Encryptor create() {
        LOGGER.debug("Creating a Kalium implementation of NaclFacadeFactory");

        final NaCl.Sodium sodium = NaCl.sodium();

        return new Kalium(sodium);
    }

}