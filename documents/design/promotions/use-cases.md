# Promo edge use cases

How to read promo descriptions:
- `[auto]` or `[coupon]` are promo types
- `[ex]` and `[nex]` are exclusive and non-exclusive
- first item before `|` is apply level: `li` for line item, `bundle`, `cart` or `shipping`.    
Not sure if this information will be a part of our model, but helps to reason about things. `bundle` is a type we haven't discussed before, and I use it here to identify promos that apply only if certain item combination is in cart
- `Q` is qualifier and `O` is offer

## Case 1: Auto-apply mess in liquor store

1. **15% off all French wines** [auto] [nex]    
    - li | Q: category = "wines → french" | O: 15% off
1. **Buy bottle of wine and five glasses, get free shipping and any corkscrew under $10 for free** [auto] [nex]    
  - bundle    
  - Q: AND
    - li | category = "wines"
    - li | category = "glasses → wine" AND quantity > 5
  - O: AND
    - shipping | free
    - li | free if category = "corkscrews" AND price < 10, applied to one
      > [@michalrus] Re: `applied to one` — could I define “get such a corkscrew for each `{5 glasses + 1 bottle}`”? If not, what else can there be, instead of `applied to one`? Is case 3.1 like that?    
      > [@anna] I rather meant that if customer adds second corkscrew to cart, it must be at full price
1. **Order beer and get a free bag of ice if order total is over $50** [auto] [nex]
  - bundle
  - Q: AND
    - li | category = "beer"
    - cart | total > 50
  - O: li | free if product = "ice", applied to one
1. **10% off orders over $200!** [auto] [nex]
  - cart | Q: total > 200 | O: 10% off

Cart

| item           | qty | discount  |
|:--------------:|:---:|:---------:|
| lalala merde   |  1  |    -15%   |
| wine glass     |  6  |      -    |
| beer pack      |  8  |      -    |
| cheap vodka    |  2  |      -    |
| corkscrew      |  1  |   -100%   |
| ice            |  2  | - / -100% |
|                |     |           |
| shipping       |     |   -100%   |
| total          |     |    -10%   |

Thoughts:
- do all of these promos apply?
- how to let user know that they can get free corkscrew and ice? Do we bother to do that via API?
- discount for ice is denoted as `- / -100%` because we need to apply 50% discount to half of items in line item. Needs tech design
- with `bundle` application level, this fits "one cart-level promo per cart" concept
- qualifiers like "all wine" are category-based; we probably should have category-based qualifier instead of saved search, for clarity. Categories should be identified by id.

## Case 2: Summer sale exclusion

1. **Summer sale: buy a sweater, get another for 50%!** [auto] [ex]
1. **10% off everything!** [coupon] [ex]    
Let's not discuss where she got that coupon.


- should warn that coupon application discards auto-apply, but not reject request?
- what should happen if user applies a coupon that gives less benefit than auto-apply? With one exclusive coupon applied manually, do we not run promo algorithm at all, or do we run a check if there exists one or multiple auto-apply promos that yield more savings?

## Case 3: Exclusive gifts

1. **Buy 5 makeup items and get a free cosmetic bag** [auto] [ex]
1. **Free shipping on orders over $100** [auto] [nex]
1. **50% off [some specific SKU] XXX mascara** [coupon] [nex]

Let's say I have 6 makeup items in cart, including XXX mascara, with total over $100.
If I don't add a bag to cart, then bundle condition is not satisfied and promo 1 isn't applied.   
Then I add a said bag to cart. If cost of the bag (and hence money saved on getting it free) makes algorithm pick promo 1, things seem logical from user perspective. But if bag is cheaper than 50% I save on mascara, then bag will be added to cart at full cost. Now, if I'm shopping online, and I'm staring at a marketing banner promising me free bag, and I am still offered to buy it at full price, I'm going to be surprised.   
What's going to be even a more exciting experience is if I for any reason detach coupon (let's say this mascara goes out of stock, so I remove it from cart, but I still have 5 items), promo 1 might kick in — now the bag is free. I proceed to checkout and select shipping. And now tables can turn once again depending on the shipping cost and algorithm can pick promo 2 over promo 1 as the best outcome for me, and now the bag is at full price again. WTF? I only want the bag if it's free! Depending on UI, going back and removing the bag can be a weird UX...

Another case here: let's say that offered bag is special and branded and blah, and you can't purchase it, only get as gift on promotions, and I would prefer to have it over free shipping (today imma instagram beauty blogger, yo...). If algorithm decides purely on monetary profit, free shipping is going to always win over gift.

My suggestion here is to make only coupons exclusive. This way, the same scenario would be perfectly adjustable depending on if I want gift or would prefer to save money instead.    
For the same setup (6 makeup items in cart, including XXX mascara, with total over $100), I can apply mascara coupon, select shipping method, see promos 2 and 3 applied, then explicitly choose to enter coupon code and get a box that would ask if I accept that applying this coupon would add a free bag but kill other promos.    
Otherwise, if we want to solve the above cases, we'd need to offer to choose which auto to apply, which is (1) more complex algorithm, API and UI and (2) not really something you can call "auto-applied"...

> [@michalrus] But maybe it’s the best (simplest) solution!
> 
> * When there’s only one available (considering both coupon and auto- types), apply it.
> 
> * When there are more (incl. when they add a coupon), tell the customer “Hey, we have several promotions available for you. Which one would you like to choose?”
>
> This way there are no (negative) surprises and they know best what’s best for them.
>
> [@anna] Yes, I agree, I've been thinking about this too, like "semi-automatic" promos or "coupons without code" :) Autosuggested promos?.. We should iterate over this
>

## Case 4: Tearing apart multi-tier

1. **Get 10% off for orders over $100!** [auto] [nex]
1. **Get 20% off for orders over $200!** [auto] [nex]

Now, this should have been implemented as a tiered promotion, but some chaotic admin can create two individual promotions like this.    
If we allow multiple auto-apply cart-level promotions and cart qualifies for both, customer will receive some mad discount. If we care to guard against this scenario, we can:
1. restrict at cart level:    
Allow only one cart-level auto-apply promotion    
What happens if cart is qualified for amount off and percent off at the same time?
1. restrict at promotion level:    
Auto-bundle (or offer to bundle) promotions with same apply type and qualifier into a multi-tier promo
