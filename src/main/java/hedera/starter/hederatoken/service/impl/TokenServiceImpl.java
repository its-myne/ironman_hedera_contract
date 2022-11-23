package hedera.starter.hederatoken.service.impl;

import com.hedera.hashgraph.sdk.*;
import hedera.starter.hederatoken.dto.TokenDto;
import hedera.starter.hederatoken.service.TokenService;
import hedera.starter.utilities.HederaClient;
import hedera.starter.utilities.PrivateKeys;
import io.github.cdimascio.dotenv.Dotenv;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
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

    public TokenId createToken(TokenDto tokenDto) throws PrecheckStatusException, TimeoutException,
            ReceiptStatusException {

        AccountId royaltyAccountID = AccountId.fromString(Objects.requireNonNull(Dotenv.load().get("COMMON_TREASURE_ID")));

        if (tokenDto != null) {

            String tokenName = tokenDto.getTokenName();
            String tokenSymbol = tokenDto.getTokenSymbol();
            String firstSellerAccountId = tokenDto.getFirstSellerAccountId();
            String firstSellerPrivateKey = tokenDto.getFirstSellerPrivateKey();

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
        return null;
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


    public Status burnToken(TokenDto tokenDto)
            throws ReceiptStatusException, PrecheckStatusException, TimeoutException {

        if (tokenDto != null) {
            String tokenId = tokenDto.getTokenId();
            Long serial = tokenDto.getSerial();
            String supplyKeyBurn = tokenDto.getSupplyKey();

            TokenBurnTransaction burnTransaction = new TokenBurnTransaction().setTokenId(TokenId.fromString(tokenId))
                    .setSerials(List.of(serial))
                    .freezeWith(client)
                    .sign(PrivateKey.fromString(supplyKeyBurn));

            TransactionResponse execute = burnTransaction.execute(client);
            TransactionReceipt receipt = execute.getReceipt(client);
            log.info("Burn NFT with serial " + serial + " : " + receipt.status);

            return receipt.status;
        }
        return null;
    }

    public String associate(TokenDto tokenDto)
            throws ReceiptStatusException, PrecheckStatusException, TimeoutException {
        if (tokenDto != null) {
            String buyerId = tokenDto.getBuyerId();
            String tokenId = tokenDto.getTokenId();
            String buyerPrivateKey = tokenDto.getBuyerPrivateKey();

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
        return null;
    }

    @Override
    public String splitRoyality() throws PrecheckStatusException, TimeoutException, ReceiptStatusException {
        AccountId treasureId = AccountId.fromString(Objects.requireNonNull(Dotenv.load().get("COMMON_TREASURE_ID")));
        PrivateKey treasureKey = PrivateKey.fromString(Objects.requireNonNull(Dotenv.load().get("COMMON_TREASURE_KEY")));

        AccountId firstFeeId = AccountId.fromString(Objects.requireNonNull(Dotenv.load().get("FIRST_FEE_ID1")));
        PrivateKey firstFeeKey = PrivateKey.fromString(Objects.requireNonNull(Dotenv.load().get("FIRST_FEE_KEY1")));

        AccountId secondId = AccountId.fromString(Objects.requireNonNull(Dotenv.load().get("SECOND_FEE_ID2")));
        PrivateKey secondFeeKey = PrivateKey.fromString(Objects.requireNonNull(Dotenv.load().get("SECOND_FEE_KEY2")));

        Hbar balance = getBalance(treasureId.toString());

        if (balance.getValue().longValue() > 2){

            // minus 2 for gas fee etc

            long splitHbar = balance.getValue().longValue() - 2L;
            double percent75 = splitHbar * 0.75;
            double percent25 = splitHbar * 0.25;

            //Transfer HBAR
            TransactionResponse sendFirstHbar = new TransferTransaction()
                    .addHbarTransfer(treasureId, Hbar.from((long) -percent75)) //Sending account
                    .addHbarTransfer(firstFeeId, Hbar.from((long) percent75)) //Receiving account
                    .execute(client);
            log.info("The transfer firstFee transaction was: " +sendFirstHbar.getReceipt(client).status);

            TransactionResponse sendSecondHbar = new TransferTransaction()
                    .addHbarTransfer(treasureId, Hbar.from((long) -percent25)) //Sending account
                    .addHbarTransfer(firstFeeId, Hbar.from((long) percent25)) //Receiving account
                    .execute(client);
            log.info("The transfer secondFee transaction was: " +sendSecondHbar.getReceipt(client).status);
        }
        return "Not enough Hbar for split";
    }

    public Status firstSellerNftTransfer(TokenDto tokenDto)
            throws PrecheckStatusException, TimeoutException, ReceiptStatusException {
        if (tokenDto != null) {
            String sellerId = tokenDto.getFirstSellerAccountId();
            String buyerId = tokenDto.getBuyerId();
            String buyerPrivateKey = tokenDto.getBuyerPrivateKey();
            String tokenId = tokenDto.getTokenId();
            Long serial = tokenDto.getSerial();
            Long price  = tokenDto.getPrice();

            AccountId sellerAccount = AccountId.fromString(sellerId);
            PrivateKey sellerKey = PrivateKey.fromString(Objects.requireNonNull(Dotenv.load().get("FIRST_SELLER_KEY")));
            AccountId buyerAccount = AccountId.fromString(buyerId);

            TransferTransaction tokenTransferTx= new TransferTransaction()
                    .addNftTransfer(new NftId(TokenId.fromString(tokenId), serial),
                            sellerAccount,
                            buyerAccount)
                    .addHbarTransfer(sellerAccount, Hbar.from(price))
                    .addHbarTransfer(buyerAccount, Hbar.from(-price))
                    .freezeWith(client)
                    .sign(sellerKey);

            TransferTransaction tokenTransferTx2Sign = tokenTransferTx.sign(PrivateKey.fromString(buyerPrivateKey));
            TransactionResponse tokenTransferSubmit = tokenTransferTx2Sign.execute(client);
            TransactionReceipt tokenTransferRx = tokenTransferSubmit.getReceipt(client);

            log.info("NFT transfer " + sellerId + " to " + buyerId + " STATUS :" + tokenTransferRx.status);

            return tokenTransferRx.status;
        }
        return null;
    }

}
