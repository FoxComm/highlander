/* @flow */

import createSearch from './search';

export const {
  searchProducts,
  toggleActive,
  forceSearch,
  reducer,
} = createSearch('searchInput');

export default reducer;
