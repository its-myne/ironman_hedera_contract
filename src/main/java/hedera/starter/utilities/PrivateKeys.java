package hedera.starter.utilities;

import com.hedera.hashgraph.sdk.PrivateKey;
import io.github.cdimascio.dotenv.Dotenv;

import java.util.Objects;

public class PrivateKeys {
    private static PrivateKey privateKeyInstance =  null;

    private PrivateKeys(String privateKey){
        privateKeyInstance = PrivateKey.fromString(Objects.requireNonNull(Dotenv.load().get(privateKey)));
    }

    public static PrivateKey getPrivateKeyInstance(String privateKey) {
        if (privateKeyInstance == null) {
            new PrivateKeys(privateKey);
        }
        return privateKeyInstance;
    }
}
