package hedera.starter.hederatoken.controller;

import com.hedera.hashgraph.sdk.*;
import hedera.starter.hederatoken.service.TokenService;
import hedera.starter.hederatoken.service.impl.TokenServiceImpl;
import io.swagger.annotations.Api;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.TimeoutException;

@RestController
@Api("Handles management of Hedera Accounts")
@RequestMapping(path = "/token")
@RequiredArgsConstructor
public class HederaTokenController {

    private final TokenService tokenService;

    @PostMapping()
    public TokenId createToken(@RequestParam String tokenName, @RequestParam String tokenSymbol,
                               @RequestParam String firstSellerAccountId, @RequestParam String firstSellerPrivateKey)
            throws ReceiptStatusException, PrecheckStatusException, TimeoutException {
        return tokenService.createToken(tokenName, tokenSymbol, firstSellerAccountId, firstSellerPrivateKey);
    }

    @PostMapping("/createAccount")
    public AccountId createAccount() throws PrecheckStatusException, TimeoutException, ReceiptStatusException {
        return tokenService.createAccount();
    }

    @GetMapping("/info")
    public TokenInfo getTokenInfo(@RequestParam String tokenId) throws PrecheckStatusException, TimeoutException {
        return tokenService.getTokenInfo(tokenId);
    }

    @GetMapping("/mint")
    public TransactionReceipt mintToken(@RequestParam String tokenId,
                                        @RequestParam String contentId) throws ReceiptStatusException, PrecheckStatusException, TimeoutException {
        return tokenService.tokenMint(tokenId, contentId);
    }

    @GetMapping("/balance")
    public Hbar getBalance(@RequestParam String accountId) throws PrecheckStatusException, TimeoutException {
        return tokenService.getBalance(accountId);
    }

    @GetMapping("/generatePrivateKey")
    public String generatePrivateKey() {
        return tokenService.generatePrivateKey();
    }

    @GetMapping("/burnToken")
    public Status burnToken(@RequestParam String tokenId, @RequestParam Long serial,
                            @RequestParam String supplyKey) throws ReceiptStatusException, PrecheckStatusException, TimeoutException {
        return tokenService.burnToken(tokenId, serial, supplyKey);
    }

    @GetMapping("/associate")
    public String associate(@RequestParam String tokenId, @RequestParam String buyerId,
                            @RequestParam String buyerPrivateKey) throws ReceiptStatusException, PrecheckStatusException, TimeoutException {
        return tokenService.associate(tokenId, buyerId, buyerPrivateKey);
    }


    @GetMapping("/transferNft")
    public Status transferNft(@RequestParam String tokenId, @RequestParam Long serial,
                              @RequestParam String sellerId, @RequestParam String buyerId,
                              @RequestParam String buyerPrivateKey, @RequestParam Long price) throws ReceiptStatusException, PrecheckStatusException, TimeoutException {
        return tokenService.firstSellerTransfer(tokenId, serial, sellerId, buyerId, buyerPrivateKey, price);
    }

}
