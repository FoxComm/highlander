import makePagination from '../pagination';

const {reducer, fetch} = makePagination('/customers', 'CUSTOMERS');

export {
  reducer as default,
  fetch
};
