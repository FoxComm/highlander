-- Shared Searches
alter domain shared_search_scope text check (value in ('customersScope', 'ordersScope', 'storeAdminsScope',
                                                        'giftCardsScope', 'productsScope', 'inventoryScope',
                                                        'promotionsScope', 'couponsScope', 'couponCodesScope',
                                                        'skusScope', 'cartsScope'));