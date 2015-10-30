'use strict';

import paginate from '../modules/pagination';

const {reducer, fetch, setState} = paginate('/orders');

export {fetch, setState};
export default reducer;
