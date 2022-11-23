package hedera.starter.hederatoken.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TokenDto {
    private String tokenName;
    private String tokenSymbol;
    private String firstSellerAccountId;
    private String firstSellerPrivateKey;
    private String tokenId;
    private Long serial;
    private String supplyKey;
    private String buyerId;
    private String buyerPrivateKey;
    private Long price;
}
