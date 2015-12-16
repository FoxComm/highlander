import _ from 'lodash';
import Api from '../../lib/api';
import { createAction, createReducer } from 'redux-act';
import { update, get } from 'sprout-data';
import makePagination from '../pagination';

const { reducer, fetch } = makePagination('/rmas', 'RMAS');

export {
  reducer as default,
  fetch as fetchRmas
};
