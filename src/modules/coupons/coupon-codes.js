
import makeLiveSearch from '../live-search';

const searchTerms = [];

const { reducer, actions } = makeLiveSearch(
  'coupons.couponCodes',
  searchTerms,
  'coupon_codes_search_view/_search',
  'couponCodesScope', {
    skipInitialFetch: true
  },
);

export {
  reducer as default,
  actions
};
