-- Shared Searches
alter domain shared_search_scope drop constraint shared_search_scope_check;

alter domain shared_search_scope add constraint shared_search_scope_check check (value in (
                                                        'customersScope', 'ordersScope', 'storeAdminsScope',
                                                        'giftCardsScope', 'productsScope', 'inventoryScope',
                                                        'promotionsScope', 'couponsScope', 'couponCodesScope',
                                                        'skusScope', 'cartsScope', 'taxonomiesScope'));
