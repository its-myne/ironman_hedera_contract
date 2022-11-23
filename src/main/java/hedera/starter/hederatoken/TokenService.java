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

    public AccountId createAccount() throws PrecheckStatusException, TimeoutException, ReceiptStatusException {
        PrivateKey newAccountPrivateKey = PrivateKey.generateED25519();
        PublicKey newAccountPublicKey = newAccountPrivateKey.getPublicKey();

        TransactionResponse newAccount = new AccountCreateTransaction()
                .setKey(newAccountPublicKey)
                .setInitialBalance( Hbar.fromTinybars(1000))
                .execute(client);
        AccountId newAccountId = newAccount.getReceipt(client).accountId;
        log.info("New accountId: " + newAccountId.toString());
        log.info("New account private key: " + newAccountPrivateKey.toString());

        return newAccountId;
    }

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

    public String associate(String tokenId, String buyerId, String buyerPrivateKey) throws ReceiptStatusException, PrecheckStatusException, TimeoutException {
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

    public Status transfer(String tokenId, Long serial, String sellerId,
                           String buyerId,String buyerPrivateKey,  Long price) throws PrecheckStatusException, TimeoutException, ReceiptStatusException {

        AccountId sellerAccount = AccountId.fromString(sellerId);
        AccountId buyerAccount = AccountId.fromString(buyerId);

        TransferTransaction tokenTransferTx2 = new TransferTransaction()
                .addNftTransfer(new NftId(TokenId.fromString(tokenId), serial),
                        sellerAccount,
                        buyerAccount)
                .addHbarTransfer(sellerAccount, Hbar.from(price))
                .addHbarTransfer(buyerAccount, Hbar.from(-price))
                .freezeWith(client)
                .sign(PrivateKey.fromString(
                        "302e020100300506032b65700422042004eb066e98fd9b6e7ca925b7cc822db6db221e5bc1cd7cdf26ec46b925869b18"
                ));

        TransferTransaction tokenTransferTx2Sign  = tokenTransferTx2.sign(PrivateKey.fromString(buyerPrivateKey));
        TransactionResponse tokenTransferSubmit2  = tokenTransferTx2Sign.execute(client);
        TransactionReceipt tokenTransferRx2 = tokenTransferSubmit2.getReceipt(client);

        System.out.println("NFT transfer Alice->Bob status" + tokenTransferRx2.status);

        return tokenTransferRx2.status;
    }

}
