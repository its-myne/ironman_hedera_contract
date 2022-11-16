package hedera.starter.utilities;

import com.hedera.hashgraph.sdk.AccountId;
import com.hedera.hashgraph.sdk.Client;
import com.hedera.hashgraph.sdk.Hbar;
import com.hedera.hashgraph.sdk.PrivateKey;
import io.github.cdimascio.dotenv.Dotenv;

import java.util.Objects;

public class HederaClient {
    /**
     * Create Singleton client instead of recreating everywhere
     */
    private static Client client = null;

    private HederaClient() {
        AccountId operatorId = AccountId.fromString(Objects.requireNonNull(Dotenv.load().get("OPERATOR_ID")));
        PrivateKey operatorKey = PrivateKey.fromString(Objects.requireNonNull(Dotenv.load().get("OPERATOR_KEY")));
        client = Client.forTestnet();
        client.setOperator(operatorId, operatorKey);
        client.setDefaultMaxTransactionFee(Hbar.from(50));
    }

    public static Client getHederaClientInstance() {
        if (client == null) {
            new HederaClient();
        }
        return client;
    }


}

