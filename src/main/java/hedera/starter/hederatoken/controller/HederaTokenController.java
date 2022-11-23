package hedera.starter.hederatoken.controller;

import com.hedera.hashgraph.sdk.*;
import hedera.starter.hederatoken.dto.TokenDto;
import hedera.starter.hederatoken.service.TokenService;
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
    public TokenId createToken(@RequestBody TokenDto tokenDto)
            throws ReceiptStatusException, PrecheckStatusException, TimeoutException {
        return tokenService.createToken(tokenDto);
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
                                        @RequestParam String contentId)
            throws ReceiptStatusException, PrecheckStatusException, TimeoutException {
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

    @PostMapping("/burnToken")
    public Status burnToken(@RequestBody TokenDto tokenDto)
            throws ReceiptStatusException, PrecheckStatusException, TimeoutException {
        return tokenService.burnToken(tokenDto);
    }

    @PostMapping("/associate")
    public String associate(@RequestBody TokenDto tokenDto)
            throws ReceiptStatusException, PrecheckStatusException, TimeoutException {
        return tokenService.associate(tokenDto);
    }

    @GetMapping("/splitRoyality")
    public String splitRoyality() throws ReceiptStatusException, PrecheckStatusException, TimeoutException {
        return tokenService.splitRoyality();
    }


    @PostMapping("/firstSellerNftTransfer")
    public Status transferNft(@RequestBody TokenDto tokenDto)
            throws ReceiptStatusException, PrecheckStatusException, TimeoutException {
        return tokenService.firstSellerNftTransfer(tokenDto);
    }

}
