package hedera.starter.hederatoken.service.impl;

import com.hedera.hashgraph.sdk.*;
import hedera.starter.hederatoken.service.TokenService;
import hedera.starter.utilities.HederaClient;
import hedera.starter.utilities.PrivateKeys;
import io.github.cdimascio.dotenv.Dotenv;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeoutException;

@Service
@Slf4j
public class TokenServiceImpl implements TokenService {

    private final Client client = HederaClient.getHederaClientInstance();
    private final PrivateKey supplyKey = PrivateKeys.getPrivateKeyInstance("supplyKey");
    private final PrivateKey adminKey = PrivateKeys.getPrivateKeyInstance("adminKey");
    private final PrivateKey pauseKey = PrivateKeys.getPrivateKeyInstance("pauseKey");
    private final PrivateKey freezeKey = PrivateKeys.getPrivateKeyInstance("freezeKey");
    private final PrivateKey wipeKey = PrivateKeys.getPrivateKeyInstance("wipeKey");

    public AccountId createAccount() throws PrecheckStatusException, TimeoutException, ReceiptStatusException {
        PrivateKey newAccountPrivateKey = PrivateKey.generateED25519();
        PublicKey newAccountPublicKey = newAccountPrivateKey.getPublicKey();

        TransactionResponse newAccount = new AccountCreateTransaction()
                .setKey(newAccountPublicKey)
                .setInitialBalance(Hbar.fromTinybars(1000))
                .execute(client);

        AccountId newAccountId = newAccount.getReceipt(client).accountId;
        assert newAccountId != null;
        log.info("New accountId: " + newAccountId);
        log.info("New account private key: " + newAccountPrivateKey);

        return newAccountId;
    }

    public TokenId createToken(String tokenName, String tokenSymbol,
                               String firstSellerAccountId, String firstSellerPrivateKey) throws PrecheckStatusException, TimeoutException,
            ReceiptStatusException {

        AccountId royaltyAccountID = AccountId.fromString(Objects.requireNonNull(Dotenv.load().get("COMMON_TREASURE_ID")));

        TokenCreateTransaction nftCreate = new TokenCreateTransaction()
                .setTokenName(tokenName)
                .setTokenSymbol(tokenSymbol)
                .setTokenType(TokenType.NON_FUNGIBLE_UNIQUE)
                .setDecimals(0)
                .setInitialSupply(0)
                .setTreasuryAccountId(AccountId.fromString(firstSellerAccountId))
                .setSupplyType(TokenSupplyType.FINITE)
                .setMaxSupply(10000)
                .setCustomFees(nftCustomFee(royaltyAccountID))
                .setAdminKey(adminKey)
                .setSupplyKey(supplyKey)
                // .setPauseKey(pauseKey)
                .setFreezeKey(freezeKey)
                .setWipeKey(wipeKey)
                .freezeWith(client)
                .sign(PrivateKey.fromString(firstSellerPrivateKey));


        TokenCreateTransaction nftCreateTxSign = nftCreate.sign(adminKey);
        TransactionResponse nftCreateSubmit = nftCreateTxSign.execute(client);
        TransactionReceipt receipt = nftCreateSubmit.getReceipt(client);
        TokenId tokenId = receipt.tokenId;
        log.info("Created NFT with Token ID: " + tokenId);

        return tokenId;
    }

    public TokenInfo getTokenInfo(String tokenId) throws PrecheckStatusException, TimeoutException {
        return new TokenInfoQuery().setTokenId(TokenId.fromString(tokenId)).execute(client);
    }

    public TransactionReceipt tokenMint(String tokenId, String contentId) throws PrecheckStatusException, TimeoutException, ReceiptStatusException {

        TokenMintTransaction tokenMintTransaction = new TokenMintTransaction()
                .setTokenId(TokenId.fromString(tokenId))
                .addMetadata(contentId.getBytes())
                .freezeWith(client);

        TokenMintTransaction mintTxSign = tokenMintTransaction.sign(supplyKey);
        TransactionResponse mintTxSubmit = mintTxSign.execute(client);
        TransactionReceipt mintRx = mintTxSubmit.getReceipt(client);

        log.info("Created NFT " + tokenId + " with serial: " + mintRx.serials.get(0));

        return mintRx;
    }

    private List<CustomFee> nftCustomFee(AccountId treasureId) {
        List<CustomFee> list = new ArrayList<>();

        CustomFee customRoyaltyFee = new CustomRoyaltyFee()
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

    public String generatePrivateKey() {
        return PrivateKey.generateED25519().toString();
    }


    public Status burnToken(String tokenId, Long serial, String supplyKey)
            throws ReceiptStatusException, PrecheckStatusException, TimeoutException {
        TokenBurnTransaction burnTransaction = new TokenBurnTransaction().setTokenId(TokenId.fromString(tokenId))
                .setSerials(List.of(serial))
                .freezeWith(client)
                .sign(PrivateKey.fromString(supplyKey));

        TransactionResponse execute = burnTransaction.execute(client);
        TransactionReceipt receipt = execute.getReceipt(client);
        log.info("Burn NFT with serial " + serial + " : " + receipt.status);

        return receipt.status;
    }

    public String associate(String tokenId, String buyerId, String buyerPrivateKey)
            throws ReceiptStatusException, PrecheckStatusException, TimeoutException {
        TokenAssociateTransaction associateBuyer = new TokenAssociateTransaction()
                .setAccountId(AccountId.fromString(buyerId))
                .setTokenIds(List.of(TokenId.fromString(tokenId)))
                .freezeWith(client)
                .sign(PrivateKey.fromString(buyerPrivateKey));
        TransactionResponse associateSubmit = associateBuyer.execute(client);
        TransactionReceipt receipt = associateSubmit.getReceipt(client);
        log.info(buyerId + "NFT Manual Association:" + receipt.status);
        return receipt.status.toString();
    }

    public Status firstSellerTransfer(String tokenId, Long serial, String sellerId,
                                      String buyerId, String buyerPrivateKey, Long price)
            throws PrecheckStatusException, TimeoutException, ReceiptStatusException {

        AccountId sellerAccount = AccountId.fromString(sellerId);
        PrivateKey sellerKey = PrivateKey.fromString(Objects.requireNonNull(Dotenv.load().get("FIRST_SELLER_KEY")));
        AccountId buyerAccount = AccountId.fromString(buyerId);

        TransferTransaction tokenTransferTx2 = new TransferTransaction()
                .addNftTransfer(new NftId(TokenId.fromString(tokenId), serial),
                        sellerAccount,
                        buyerAccount)
                .addHbarTransfer(sellerAccount, Hbar.from(price))
                .addHbarTransfer(buyerAccount, Hbar.from(-price))
                .freezeWith(client)
                .sign(sellerKey);

        TransferTransaction tokenTransferTx2Sign = tokenTransferTx2.sign(PrivateKey.fromString(buyerPrivateKey));
        TransactionResponse tokenTransferSubmit = tokenTransferTx2Sign.execute(client);
        TransactionReceipt tokenTransferRx2 = tokenTransferSubmit.getReceipt(client);

        log.info("NFT transfer " + sellerId + " to " + buyerId + " STATUS :" + tokenTransferRx2.status);

        return tokenTransferRx2.status;
    }

}
