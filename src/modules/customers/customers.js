'use strict';

import makePagination from '../pagination';

const {reducer, actions: {fetch, setFetchData}} = makePagination('/customers', 'CUSTOMERS');

export default reducer;

export {
  fetch,
  setFetchData
};
