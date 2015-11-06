'use strict';

import makePagination from '../modules/pagination';

const {reducer, actions: {fetch, setFetchParams}} = makePagination('/orders', 'ORDERS');

export default reducer;

export {
  fetch,
  setFetchParams
};
