package hedera.starter.hederatoken;

import com.hedera.hashgraph.sdk.*;
import io.swagger.annotations.Api;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.TimeoutException;

@RestController
@Api("Handles management of Hedera Accounts")
@RequestMapping(path = "/token")
public class HederaTokenController {

    private final TokenService tokenService;

    public HederaTokenController(TokenService tokenService) {
        this.tokenService = tokenService;
    }

    @PostMapping()
    public TokenId createToken(@RequestParam String tokenName, @RequestParam String tokenSymbol,
                               @RequestParam String treasureId, @RequestParam String privateKey,
                               @RequestParam String royaltyId)
            throws ReceiptStatusException, PrecheckStatusException, TimeoutException {
        return tokenService.createToken(tokenName, tokenSymbol, treasureId, privateKey, royaltyId);
    }

    @GetMapping("/info")
    public TokenInfo getTokenInfo(@RequestParam String tokenId) throws PrecheckStatusException, TimeoutException {
        return tokenService.getTokenInfo(tokenId);
    }

    @GetMapping("/mint")
    public TransactionReceipt mintToken(@RequestParam String tokenId, @RequestParam String CID) throws ReceiptStatusException, PrecheckStatusException, TimeoutException {
        return  tokenService.tokenMint(tokenId,CID);
    }

    @GetMapping("/balance")
    public Hbar getBalance(@RequestParam String accountId) throws PrecheckStatusException, TimeoutException {
           return tokenService.getBalance(accountId);
    }

    @GetMapping("/generatePrivateKey")
    public String generatePrivateKey(){
        return tokenService.generatePrivateKey();
    }

    @GetMapping("/burnToken")
    public Status burnToken(@RequestParam String tokenId,@RequestParam Long serial,
                            @RequestParam String supplyKey) throws ReceiptStatusException, PrecheckStatusException, TimeoutException {
        return tokenService.burnToken(tokenId,serial, supplyKey);
    }

}
