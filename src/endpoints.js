
const MAX_RESULTS = 1000;

// auth endpoints
export const login = '/v1/public/login';
export const signup ='/v1/public/registrations/new';
export const googleSignin = '/v1/public/signin/google/customer';
export const logout = '/v1/public/logout';

// product endpoints
export const search = `/v1/search/products_catalog_view/_search?size=${MAX_RESULTS}`;

// cart endpoints
export const addToCart = '/v1/my/cart/add';
export const updateQty = '/v1/my/cart/line-items';
export const removeFromCart = '/v1/my/cart/line-items/:id/edit';

// address endpoints
export const addresses = '/v1/my/addresses';
export const address = addressId => `/v1/my/addresses/${addressId}`;
export const addressDefault = addressId => `${address(addressId)}/default`;
export const addressesDefault = '/v1/my/address/default';

// payment methods
export const creditCards = '/v1/my/payment-methods/credit-cards';
export const creditCardsDefault = `${creditCards}/default`;
