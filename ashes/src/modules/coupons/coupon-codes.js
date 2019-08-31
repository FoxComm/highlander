import makeLiveSearch from '../live-search';

const searchTerms = [
  {
    title: 'Coupon Code : Code',
    type: 'identifier',
    term: 'code',
  },
  {
    title: 'Coupon Code : Total Uses',
    type: 'number',
    term: 'totalUses',
  },
  {
    title: 'Coupon Code : Current Carts',
    type: 'number',
    term: 'currentCarts',
  },
  {
    title: 'Coupon : Date/Time Created',
    type: 'date',
    term: 'createdAt',
  }
];

const { reducer, actions } = makeLiveSearch(
  'coupons.couponCodes',
  searchTerms,
  'coupon_codes_search_view',
  'couponCodesScope', {
    skipInitialFetch: true
  },
);

export {
  reducer as default,
  actions
};
