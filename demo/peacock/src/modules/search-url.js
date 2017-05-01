/* @flow */

import createSearch from './search';

export const {
  searchProducts,
  toggleActive,
  forceSearch,
  reducer,
} = createSearch('searchUrl');

export default reducer;
