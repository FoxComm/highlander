'use strict';

import makePagination from '../modules/pagination';

const {reducer, actions: {fetch, setFetchData}} = makePagination('/orders', 'ORDERS');

export default reducer;

export {
  fetch,
  setFetchData
};
