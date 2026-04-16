package com.game.magic;

import java.util.Map;

public class AdjustCocosEventPayload {
    public String eventToken;
    public Double revenue;
    public String currency;
    public String orderId;
    public String deduplicationId;
    public String callbackId;
    public String productId;
    public Map<String, String> callbackParams;
    public Map<String, String> partnerParams;
}