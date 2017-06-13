# Vocab
- `cart metadata` or `metadata` - all the data from cart that algorithm needs to make decision    
Something along    
```json
{
  "total": 12313,
  "shippingCost": 123,
  "lineItems": [
    {
      "categoryId": 4,
      "skuId": 76,
      "unitPrice": 876,
      "quantity": 2,
      "type": "regular"
    }
  ],
  "customerGroupIds": [1, 2, 3],
  "appliedCouponIds": []
}
```
- `ex` and `nex` promos are exclusive and non-exclusive accordingly

# General considerations
- to optimize for performance, auto-apply promo data must be kept in memory and backed up to db. Use `State` monad?
  > [@michalrus] I think invalidating all of RAM cache would be safest. So if any admin changes anything regarding promotions → invalidate RAM → re-read the rules from the DB. This way we don’t have to maintain both fresh reading & updating code, the first one suffices. =)    
  > [@anna] We'll get a ton or re-reads on a multitenant system...
- use `.par` (Scala parallel collection processing) when iterating through auto-apply promo data

_this document is to be filled after some design decisions are made_
