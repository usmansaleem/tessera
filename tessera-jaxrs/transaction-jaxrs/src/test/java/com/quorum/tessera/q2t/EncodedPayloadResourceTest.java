package com.quorum.tessera.q2t;

import com.quorum.tessera.api.PayloadDecryptRequest;
import com.quorum.tessera.api.PayloadEncryptResponse;
import com.quorum.tessera.api.SendRequest;
import com.quorum.tessera.data.MessageHash;
import com.quorum.tessera.enclave.*;
import com.quorum.tessera.encryption.PublicKey;
import com.quorum.tessera.transaction.EncodedPayloadManager;
import com.quorum.tessera.transaction.ReceiveResponse;
import com.quorum.tessera.transaction.TransactionManager;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import javax.ws.rs.core.Response;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class EncodedPayloadResourceTest {

    private TransactionManager transactionManager;

    private EncodedPayloadManager encodedPayloadManager;

    private EncodedPayloadResource encodedPayloadResource;

    @Before
    public void onSetup() throws Exception {
        this.transactionManager = mock(TransactionManager.class);
        this.encodedPayloadManager = mock(EncodedPayloadManager.class);

        encodedPayloadResource = new EncodedPayloadResource(encodedPayloadManager, transactionManager);

    }

    @After
    public void onTearDown() throws Exception {
        verifyNoMoreInteractions(transactionManager,encodedPayloadManager);

    }

    @Test
    public void createPayload() {
        final String base64Key = "BULeR8JyUWhiuuCMU/HLA0Q5pzkYT+cHII3ZKBey3Bo=";

        final SendRequest sendRequest = new SendRequest();
        sendRequest.setPayload(Base64.getEncoder().encode("PAYLOAD".getBytes()));
        sendRequest.setTo(base64Key);
        sendRequest.setAffectedContractTransactions("dHgx");

        final PublicKey sender = PublicKey.from(Base64.getDecoder().decode("oNspPPgszVUFw0qmGFfWwh1uxVUXgvBxleXORHj07g8="));
        when(transactionManager.defaultPublicKey()).thenReturn(sender);

        final EncodedPayload samplePayload = EncodedPayload.Builder.create()
            .withSenderKey(sender)
            .withRecipientKeys(List.of(PublicKey.from(Base64.getDecoder().decode(base64Key))))
            .withRecipientBoxes(List.of("boxOne".getBytes()))
            .withRecipientNonce("recipientNonce".getBytes())
            .withCipherText("testPayload".getBytes())
            .withCipherTextNonce("cipherTextNonce".getBytes())
            .withPrivacyMode(PrivacyMode.STANDARD_PRIVATE)
            .withAffectedContractTransactions(Map.of(TxHash.from("tx1".getBytes()), "tx1val".getBytes()))
            .withExecHash(new byte[0])
            .build();

        when(encodedPayloadManager.create(any(com.quorum.tessera.transaction.SendRequest.class)))
            .thenReturn(samplePayload);


        final Response result = encodedPayloadResource.createEncodedPayload(sendRequest);

        assertThat(result.getStatus()).isEqualTo(200);

        final PayloadEncryptResponse payloadEncryptResponse = PayloadEncryptResponse.class.cast(result.getEntity());

        assertThat(PublicKey.from(payloadEncryptResponse.getSenderKey())).isEqualTo(sender);
        assertThat(payloadEncryptResponse.getCipherText()).isEqualTo("testPayload".getBytes());
        assertThat(payloadEncryptResponse.getCipherTextNonce()).isEqualTo("cipherTextNonce".getBytes());
        assertThat(payloadEncryptResponse.getRecipientBoxes()).hasSize(1).containsExactly("boxOne".getBytes());
        assertThat(payloadEncryptResponse.getRecipientNonce()).isEqualTo("recipientNonce".getBytes());
        assertThat(payloadEncryptResponse.getRecipientKeys()).hasSize(1);
        assertThat(payloadEncryptResponse.getRecipientKeys().get(0)).isEqualTo(Base64.getDecoder().decode(base64Key));
        assertThat(payloadEncryptResponse.getPrivacyMode()).isEqualTo(0);
        assertThat(payloadEncryptResponse.getAffectedContractTransactions()).contains(entry("dHgx", "dHgxdmFs"));
        assertThat(payloadEncryptResponse.getExecHash()).isEmpty();

        final ArgumentCaptor<com.quorum.tessera.transaction.SendRequest> argumentCaptor =
            ArgumentCaptor.forClass(com.quorum.tessera.transaction.SendRequest.class);

        verify(encodedPayloadManager).create(argumentCaptor.capture());
        verify(transactionManager).defaultPublicKey();

        com.quorum.tessera.transaction.SendRequest businessObject = argumentCaptor.getValue();
        assertThat(businessObject).isNotNull();
        assertThat(businessObject.getPayload()).isEqualTo(sendRequest.getPayload());
        assertThat(businessObject.getSender()).isEqualTo(sender);
        assertThat(businessObject.getRecipients()).hasSize(1);
        assertThat(businessObject.getRecipients().get(0).encodeToBase64()).isEqualTo(base64Key);
        assertThat(businessObject.getPrivacyMode()).isEqualTo(PrivacyMode.STANDARD_PRIVATE);
        assertThat(businessObject.getAffectedContractTransactions()).containsExactly(new MessageHash("tx1".getBytes()));
        assertThat(businessObject.getExecHash()).isEmpty();
    }


    @Test
    public void decryptPayload() {

        final PrivacyMode privacyMode = PrivacyMode.PRIVATE_STATE_VALIDATION;

        final Base64.Decoder decoder = Base64.getDecoder();

        final PayloadDecryptRequest request = new PayloadDecryptRequest();
        request.setSenderKey(decoder.decode("BULeR8JyUWhiuuCMU/HLA0Q5pzkYT+cHII3ZKBey3Bo="));
        request.setCipherText(decoder.decode("h7av/vhPlaPFECB1K30hNWugv/Bu"));
        request.setCipherTextNonce(decoder.decode("8MVXAESCQuRHWxrQ6b5MXuYApjia+2h0"));
        request.setRecipientBoxes(List.of(decoder.decode("FNirZRc2ayMaYopCBaWQ/1I7VWFiCM0lNw533Hckzxb+qpvngdWVVzJlsE05dbxl")));
        request.setRecipientNonce(decoder.decode("p9gYDJlEoBvLdUQ+ZoONl2Jl9AirV1en"));
        request.setRecipientKeys(List.of(decoder.decode("BULeR8JyUWhiuuCMU/HLA0Q5pzkYT+cHII3ZKBey3Bo=")));
        request.setPrivacyMode(privacyMode.getPrivacyFlag());
        request.setAffectedContractTransactions(Map.of("dHgx", "dHgxdmFs", "dHgy", "dHgydmFs"));
        request.setExecHash("execHash".getBytes());

        final ReceiveResponse response = mock(ReceiveResponse.class);
        when(response.getPrivacyMode()).thenReturn(privacyMode);
        when(response.getUnencryptedTransactionData()).thenReturn("decryptedData".getBytes());
        when(response.getExecHash()).thenReturn("I Love sparrows".getBytes());

        MessageHash messageHash = mock(MessageHash.class);
        when(messageHash.getHashBytes()).thenReturn("SomeMessageHashBytes".getBytes());

        when(response.getAffectedTransactions()).thenReturn(Set.of(messageHash));

        when(encodedPayloadManager.decrypt(any(), eq(null))).thenReturn(response);

        final Response result = encodedPayloadResource.decryptEncodedPayload(request);

        assertThat(result.getStatus()).isEqualTo(200);

        final com.quorum.tessera.api.ReceiveResponse payloadEncryptResponse
            = com.quorum.tessera.api.ReceiveResponse.class.cast(result.getEntity());

        assertThat(payloadEncryptResponse.getPayload()).isEqualTo("decryptedData".getBytes());
        assertThat(payloadEncryptResponse.getPrivacyFlag()).isEqualTo(privacyMode.getPrivacyFlag());
        assertThat(payloadEncryptResponse.getAffectedContractTransactions())
            .contains(Base64.getEncoder().encodeToString("SomeMessageHashBytes".getBytes()));

        assertThat(payloadEncryptResponse.getExecHash()).isEqualTo("I Love sparrows");

        verify(encodedPayloadManager).decrypt(any(), eq(null));

    }

}