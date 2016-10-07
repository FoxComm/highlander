
const MAX_RESULTS = 1000;

// auth endpoints
export const login = '/v1/public/login';
export const signup ='/v1/public/registrations/new';
export const googleSignin = '/v1/public/signin/google/customer';
export const logout = '/v1/public/logout';

// product endpoints
export const search = `/search/products_catalog_view/_search?size=${MAX_RESULTS}`;

// cart endpoints
export const cart = '/v1/my/cart';
export const cartCheckout = '/v1/my/cart/checkout';
export const shippingMethods = '/v1/my/cart/shipping-methods';
export const shippingMethod = '/v1/my/cart/shipping-method';
export const shippingAddress = '/v1/my/cart/shipping-address';
export const shippingAddressId = id => `${shippingAddress}/id`;
export const cartLineItems = '/v1/my/cart/line-items';
export const cartPaymentCreditCarts = '/v1/my/cart/payment-methods/credit-cards';
export const cartPaymentGiftCards = '/v1/my/cart/payment-methods/gift-cards';
export const cartPaymentStoreCredits = '/v1/my/cart/payment-methods/store-credits';

export const addToCart = '/v1/my/cart/add';

export const removeFromCart = '/v1/my/cart/line-items/:id/edit';

// address endpoints
export const addresses = '/v1/my/addresses';
export const address = addressId => `/v1/my/addresses/${addressId}`;
export const addressDefault = addressId => `${address(addressId)}/default`;
export const addressesDefault = '/v1/my/address/default';

// payment methods, credit cards
export const creditCards = '/v1/my/payment-methods/credit-cards';
export const creditCard = creditCardId => `${creditCards}/${creditCardId}`;
export const creditCardDefault = creditCardId => `${creditCard(creditCardId)}/default`;

// payment methods, store credits
export const storeCredit = storeCreditId => `/v1/my/payment-methods/store-credits/${storeCreditId}`;
export const storeCreditTotals = `/v1/my/payment-methods/store-credits/totals`;
export const storeCredits = `/search/store_credits_search_view/_search`;
