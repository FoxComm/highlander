
import makeLiveSearch from '../live-search';

const searchTerms = [
  {
    title: 'Coupon : Name',
    type: 'string',
    term: 'name',
  },
  {
    title: 'Coupon : Storefront Name',
    type: 'string',
    term: 'storefrontName',
  },
  {
    title: 'Coupon : Code',
    type: 'string',
    term: 'code',
  },
  {
    title: 'Coupon : Date/Time Created',
    type: 'date',
    term: 'createdAt',
  },
];

const { reducer, actions } = makeLiveSearch(
  'coupons.list',
  searchTerms,
  'coupons_search_view/_search',
  'couponsScope',
  {
    initialState: { sortBy: '-createdAt' }
  }
);

export {
  reducer as default,
  actions
};
