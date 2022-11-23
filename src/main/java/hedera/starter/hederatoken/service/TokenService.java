package hedera.starter.hederatoken.service;

import com.hedera.hashgraph.sdk.*;
import hedera.starter.hederatoken.dto.TokenDto;

import java.util.concurrent.TimeoutException;

public interface TokenService {
    AccountId createAccount() throws PrecheckStatusException, TimeoutException, ReceiptStatusException;

    TokenId createToken(TokenDto tokenDto)
            throws PrecheckStatusException, TimeoutException, ReceiptStatusException;

    TokenInfo getTokenInfo(String tokenId) throws PrecheckStatusException, TimeoutException;

    TransactionReceipt tokenMint(String tokenId, String contentId)
            throws PrecheckStatusException, TimeoutException, ReceiptStatusException;

    Hbar getBalance(String accountId) throws PrecheckStatusException, TimeoutException;

    String generatePrivateKey();

    Status burnToken(TokenDto tokenDto)
            throws ReceiptStatusException, PrecheckStatusException, TimeoutException;

    String associate(TokenDto tokenDto)
            throws ReceiptStatusException, PrecheckStatusException, TimeoutException;

    String splitRoyality() throws PrecheckStatusException, TimeoutException, ReceiptStatusException;

    Status firstSellerNftTransfer(TokenDto tokenDto)
            throws PrecheckStatusException, TimeoutException, ReceiptStatusException;

    //TODO
    // second marketplace I will be use scheduled transaction,
    // at the moment second seller can sell by secureTrade Hashpack wallet
}
