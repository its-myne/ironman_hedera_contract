package hedera.starter.hederatoken.service;

import com.hedera.hashgraph.sdk.*;

import java.util.concurrent.TimeoutException;

public interface TokenService {
    AccountId createAccount() throws PrecheckStatusException, TimeoutException, ReceiptStatusException;

    TokenId createToken(String tokenName, String tokenSymbol,
                        String firstSellerAccountId, String firstSellerPrivateKey)
            throws PrecheckStatusException, TimeoutException, ReceiptStatusException;

    TokenInfo getTokenInfo(String tokenId) throws PrecheckStatusException, TimeoutException;

    TransactionReceipt tokenMint(String tokenId, String contentId)
            throws PrecheckStatusException, TimeoutException, ReceiptStatusException;

    Hbar getBalance(String accountId) throws PrecheckStatusException, TimeoutException;

    String generatePrivateKey();

    Status burnToken(String tokenId, Long serial, String supplyKey)
            throws ReceiptStatusException, PrecheckStatusException, TimeoutException;

    String associate(String tokenId, String buyerId, String buyerPrivateKey)
            throws ReceiptStatusException, PrecheckStatusException, TimeoutException;

    Status firstSellerTransfer(String tokenId, Long serial, String sellerId,
                    String buyerId,String buyerPrivateKey,  Long price)
            throws PrecheckStatusException, TimeoutException, ReceiptStatusException;

    //TODO
    // second marketplace I will be use scheduled transaction,
    // at the moment second seller can sell by secureTrade Hashpack wallet
}
