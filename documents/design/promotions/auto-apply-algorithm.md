# Auto-apply selection algorithms

## For only non-exclusive promos
Iterate over all auto-apply promos in parallel, run the following:
1. filter eligible qualifiers by type — if a promo requires some customer groups and metadata has none, discard promo immediately
2. group qualifiers by type and fix the "upper boundary" — i.e. if there are multiple promos with "cart total" qualifier of $100, $200, $300, discard $200 and $300 qualified promos if cart total is $150 

   > [@michalrus] I’m not sure if such optimizations would be necessary, if we kept all the rules & the whole cart in RAM. After all these are some simple arithmetics. Hmm.    
   > [@anna] Well, why would you want to run arithmetics if cart metadata isn't qualified for the promo?
3. apply offers from resulting promos to metadata and collect results

After this, stop parallel computation

4. group results by offer type
5. narrow down set of offers to
- best of each of
  - amount off cart
  - percent off cart
  - shipping
- all off
  - line item offers
  - gift offers

This algorithm can have two different behaviors regarding combining offers — we need to make a choice which one is correct. 
- if offer is gift and discounted shipping, and cart is qualified for free shipping by some other promo, we obviously want only free shipping in resulting offer set
- if offer is gift and cart percent off, and cart is qualified for another percent off, and qualifier types are different, do we want to
  - pick best?
  - sum percent offs?
  - ~apply them separately?~ (this is too ambiguous tho)

## With exclusive auto-apply
See [this use case](https://github.com/FoxComm/highlander/blob/master/documents/design/promotions/use-cases.md#case-3-exclusive-gifts) where I argue they are confusing, but anyways    
1-3. same as above    
4. group promos by exclusivity — non-exclusive in one list, all exclusive separate from each other    
5. pick best non-exclusives    
6. apply offers to metadata    
7. compare savings on all nex promos with savings on each ex promo and yield best
