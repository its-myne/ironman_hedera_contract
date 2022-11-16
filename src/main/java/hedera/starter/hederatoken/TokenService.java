package hedera.starter.hederatoken;

import com.hedera.hashgraph.sdk.*;
import hedera.starter.utilities.HederaClient;
import hedera.starter.utilities.PrivateKeys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;

@Service
@Slf4j
public class TokenService {

    private final Client client = HederaClient.getHederaClientInstance();
    private final PrivateKey supplyKey = PrivateKeys.getPrivateKeyInstance("supplyKey");
    private final PrivateKey adminKey = PrivateKeys.getPrivateKeyInstance("adminKey");
    private final PrivateKey pauseKey = PrivateKeys.getPrivateKeyInstance("pauseKey");
    private final PrivateKey freezeKey = PrivateKeys.getPrivateKeyInstance("freezeKey");
    private final PrivateKey wipeKey = PrivateKeys.getPrivateKeyInstance("wipeKey");

    public TokenId createToken(String tokenName, String tokenSymbol,
                               String treasureAccountId, String treasurePrivateKey,
                               String royaltyAccountID) throws PrecheckStatusException, TimeoutException, ReceiptStatusException {

        TokenCreateTransaction nftCreate = new TokenCreateTransaction()
                .setTokenName(tokenName)
                .setTokenSymbol(tokenSymbol)
                .setTokenType(TokenType.NON_FUNGIBLE_UNIQUE)
                .setDecimals(0)
                .setInitialSupply(0)
                .setTreasuryAccountId(AccountId.fromString(treasureAccountId))
                .setSupplyType(TokenSupplyType.FINITE)
                .setMaxSupply(10000)
                .setCustomFees(nftCustomFee(AccountId.fromString(royaltyAccountID)))
		        .setAdminKey(adminKey)
                .setSupplyKey(supplyKey)
                // .setPauseKey(pauseKey)
                .setFreezeKey(freezeKey)
                .setWipeKey(wipeKey)
                .freezeWith(client)
                .sign(PrivateKey.fromString(treasurePrivateKey));


        TokenCreateTransaction nftCreateTxSign  = nftCreate.sign(adminKey);
        TransactionResponse nftCreateSubmit  = nftCreateTxSign.execute(client);
        TransactionReceipt receipt = nftCreateSubmit.getReceipt(client);
        TokenId tokenId = receipt.tokenId;
        log.info("Created NFT with Token ID: " + tokenId);

        return tokenId;
    }

    public TokenInfo getTokenInfo(String tokenId) throws PrecheckStatusException, TimeoutException {
        return new TokenInfoQuery().setTokenId(TokenId.fromString(tokenId)).execute(client);
    }

    public TransactionReceipt tokenMint(String tokenId, String CID) throws PrecheckStatusException, TimeoutException, ReceiptStatusException {

        TokenMintTransaction tokenMintTransaction = new TokenMintTransaction()
                .setTokenId(TokenId.fromString(tokenId))
                .addMetadata(CID.getBytes())
                .freezeWith(client);

        TokenMintTransaction mintTxSign  = tokenMintTransaction.sign(supplyKey);
        TransactionResponse mintTxSubmit  = mintTxSign.execute(client);
        TransactionReceipt mintRx = mintTxSubmit.getReceipt(client);

        log.info("Created NFT "+ tokenId + " with serial: " + mintRx.serials.get(0));

        return mintRx;
    }

    public List<CustomFee> nftCustomFee(AccountId treasureId){
        List<CustomFee> list = new ArrayList<>();

        CustomFee customRoyaltyFee =  new CustomRoyaltyFee()
                .setNumerator(1)
                .setDenominator(10)
                .setFeeCollectorAccountId(treasureId)
                .setFallbackFee(new CustomFixedFee().setHbarAmount(new Hbar(30)));

        list.add(customRoyaltyFee);

        return list;
    }

    public Hbar getBalance(String accountId) throws PrecheckStatusException, TimeoutException {
        AccountBalance execute = new AccountBalanceQuery().setAccountId(AccountId.fromString(accountId))
                .execute(client);
        return execute.hbars;
    }

    public String generatePrivateKey(){
        return PrivateKey.generateED25519().toString();
    }


    public Status burnToken(String tokenId, Long serial, String supplyKey) throws ReceiptStatusException, PrecheckStatusException, TimeoutException {
        TokenBurnTransaction burnTransaction = new TokenBurnTransaction().setTokenId(TokenId.fromString(tokenId))
                .setSerials(List.of(serial))
                .freezeWith(client)
                .sign(PrivateKey.fromString(supplyKey));

        TransactionResponse execute = burnTransaction.execute(client);
        TransactionReceipt receipt = execute.getReceipt(client);
        log.info("Burn NFT with serial " + serial + " : " + receipt.status);

        return receipt.status;
    }


}
