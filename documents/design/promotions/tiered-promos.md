# Tiered promos
See [this use case](https://github.com/FoxComm/highlander/blob/master/documents/design/promotions/use-cases.md#case-4-tearing-apart-multi-tier)

## Definition
Tiered promotions are the correct way to express promo structure like
- Spend $100 and get 5% off
- Spend $200 and get 10% off
etc    
In this case, both qualifier and offer types match on both promos.

Another use case that I think is totally valid:
- Spend $100 and get free shipping
- Spend $200 and get free shipping and a gift    
Here qualifier types match, but offers are different

An invalid tiered promotion would probably be something like
- Spend $100 and get free shipping
- Buy 10 items of X and get 10% off on X     
Qualifier types are totally different, and don't make sense as tiers.

So, for tiered promotion definition so far, I'd say that tiered promotion is an abstraction encompassing several promotions with
- same qualifier type
- different qualifier values
- arbitrary offer types and values

## Implementation
I can see us doing one of the two:
1. explicitly define an entity for tiered promotion
2. have implicit tiered promotions

Note: irregardless of data model, _all existing_ promotions have to be traversed to find the best one, independent of whether they are a part of tiered promo or not.

I don't want to spell it out every time, so "$AMOUNT-PERCENT%" means "Spend $AMOUNT, get PERCENT% off"

### Explicit tiers
If we have a separate UI and API for tiered promo management, we would expect admins to use it to create tiered promos. Now, what happens if instead of creating a promo tier (adding $200-10% as second tier to $100-5%), someone unintentionally creates a separate $200-10% promo? Then depending on our design choices, customer either gets 5+10=15% discount, or only 10% discount if we restrict that.   
We can build a UI and API (extra effort!!! D:) that would suggest that newly created promo can be added as a tier to not-yet-existing-but-we-will-create-it-for-you tiered promotion.   
The question is, would someone want to reply "no, I want both $100-5% and $200-10% exist separately"? If so, we'd have to have some extensive documentation of what will exactly happen. If there comes another "untiered tier" of $300-15%, total discount can suddenly come up to be 5+10+15=20%, which might not be desirable for retailer.

### Implicit tiers
Given the definition of tiered promotion, we can group promotions into tiers automatically by searching for promos with same qualifier type. Then, if admin creates these promos:
- $100 - 5%
- $200 - 10% + gift
- $300 - 20% + free shipping    
and we take this into account in algorithm design, shopper with $300+ cart should end up only with the last promotion applied.

## Kickstarter model
Examples above were simple models with independent offers. Consider this Kickstarter-style tiered promo:
- Spend $100 and get 5% off on selected items
- Spend $200 and get **all of the above plus** free shipping
- Spend $300 and get **all of the above plus** gift

Is this a valid use case we want to support?    
Depending on (a) promotions mutability and (b) whether we want to support "live" links between tiers (here b depends on a), we can end up in several states, BUT I am not going to dive into that because I don't think we want to make important promo details mutable... right?    
If that is not the case, we can simply copy the offer from tier 1 when tier 2 is created. Although I can see this scenario becoming a complete mess with improper UI, Bree probably needs to input on this ;)
