
const MAX_RESULTS = 1000;

// auth endpoints
export const login = '/public/login';
export const signup ='/public/registrations/new';

// product endpoints
export const search = `/search/products_catalog_view/_search?size=${MAX_RESULTS}`;

// cart endpoints
export const addToCart = '/my/cart/add';
export const updateQty = '/my/cart/line-items';
export const removeFromCart = '/my/cart/line-items/:id/edit';

// address endpoints
export const addresses = '/my/addresses';
export const address = addressId => `/my/addresses/${addressId}`;
export const addressDefault = addressId => `${address(addressId)}/default`;
export const addressesDefault = '/my/address/default';
