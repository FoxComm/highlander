/* @flow */

// libs
import get from 'lodash/get';
import { createReducer, createAction } from 'redux-act';

// helpers
import Api from 'lib/api';
import * as search from 'lib/search';
import { post } from 'lib/search';
import criterions, { getCriterion, getWidget } from 'paragons/customer-groups/criterions';
import { aggregations } from 'elastic/request';
import { createAsyncActions } from '@foxcomm/wings';
import requestAdapter from './request-adapter';

const initialState = {
  id: null,
  type: null,
  name: null,
  mainCondition: null,
  conditions: [],
  isValid: false,
  filterTerm: null,
  createdAt: null,
  updatedAt: null,
  stats: {
    ordersCount: 0,
    totalSales: 0,
    averageOrderSize: 0,
    averageOrderSum: 0,
  }
};

/**
 * Internal actions
 */
const _fetchGroup = createAsyncActions('fetchCustomerGroup', (groupId: number) => Api.get(`/groups/${groupId}`));

const _saveGroup = createAsyncActions(
  'saveCustomerGroup',
  (groupId, data) => {
    return new Promise((resolve, reject) => {
      post('customers_search_view/_count', data.elasticRequest)
        .then(response => {
          data.customersCount = response.count;

          let request;
          if (groupId) {
            request = Api.patch(`/groups/${groupId}`, data);
          } else {
            request = Api.post('/groups', data);
          }

          request
            .then(data => resolve(data))
            .catch(err => reject(err));
        })
        .catch(err => reject(err));
    });
  }
);

const _archiveGroup = createAsyncActions('archiveCustomerGroup', (groupId: number) => Api.delete(`/groups/${groupId}`));

const _fetchStats = createAsyncActions('fetchStatsCustomerGroup', request =>
  search.post('customers_search_view/_search?size=0', request)
);

/**
 * External actions
 */

/**
 * Reset customer group to initial state
 */
export const reset = createAction(`CUSTOMER_GROUP_RESET`);
export const setName = createAction('CUSTOMER_GROUP_SET_NAME');
export const setMainCondition = createAction('CUSTOMER_GROUP_SET_MAIN_CONDITION');
export const setConditions = createAction('CUSTOMER_GROUP_SET_CONDITIONS');
export const setFilterTerm = createAction('CUSTOMER_GROUP_SET_FILTER_TERM');
export const setGroupStats = createAction('CUSTOMER_GROUP_SET_GROUP_STATS');

export const clearFetchErrors = _fetchGroup.clearErrors;
export const clearSaveErrors = _saveGroup.clearErrors;
export const clearArchiveErrors = _archiveGroup.clearErrors;

/**
 * Fetch customer group
 *
 * @param {Number} groupId Customer group id
 *
 * @return Promise
 */
export const fetchGroup = (groupId: number) => (dispatch: Function) => dispatch(_fetchGroup.perform(groupId));

/**
 * Archive customer group (soft delete)
 *
 * @param {Number} groupId Customer group id
 *
 * @return Promise
 */
export const archiveGroup = (groupId: number) => (dispatch: Function) => dispatch(_archiveGroup.perform(groupId));

/**
 * Save or create customer group
 *
 * @return Promise
 */
export const saveGroup = () => (dispatch: Function, getState: Function) => {
  const state = getState();
  const getValue = (name) => get(state, ['customerGroups', 'details', 'group', name]);

  const groupId = getValue('id');
  const name = getValue('name');
  const mainCondition = getValue('mainCondition');
  const conditions = getValue('conditions');
  const elasticRequest = requestAdapter(criterions, mainCondition, conditions).toRequest();

  const data = {
    name,
    clientState: {
      mainCondition,
      conditions,
    },
    elasticRequest,
    customersCount: 0,
  };

  return dispatch(_saveGroup.perform(groupId, data));
};

/**
 * Fetch customer group's stats
 *
 * @return Promise
 */
export const fetchGroupStats = () => (dispatch: Function, getState: Function) => {
  const state = getState();

  const mainCondition = get(state, ['customerGroups', 'details', 'group', 'mainCondition']);
  const conditions = get(state, ['customerGroups', 'details', 'group', 'conditions']);

  const request = requestAdapter(criterions, mainCondition, conditions);

  request.aggregations
    .add(new aggregations.Count('ordersCount', 'orders.referenceNumber'))
    .add(new aggregations.Sum('totalSales', 'revenue'))
    .add(new aggregations.Average('averageOrderSize', 'orders.itemsCount'))
    .add(new aggregations.Average('averageOrderSum', 'orders.grandTotal'));

  dispatch(_fetchStats.perform(request.toRequest()));
};

const validateConditions = conditions => conditions.length && conditions.every(validateCondition);

const validateCondition = ([field, operator, value]) => {
  if (!field || !operator) {
    return false;
  }

  const criterion = getCriterion(field);
  const { isValid } = getWidget(criterion, operator);

  return isValid(value, criterion);
};

type State = {
  group: TCustomerGroup;
}

const setData = (state: State, { id, type, name, createdAt, updatedAt, clientState: { mainCondition, conditions } }) => {
  return {
    ...state,
    id,
    type,
    name,
    createdAt,
    updatedAt,
    mainCondition,
    conditions,
    isValid: validateConditions(conditions),
  };
};

const reducer = createReducer({
  [reset]: (state: State) => initialState,
  [_fetchGroup.succeeded]: setData,
  [_saveGroup.succeeded]: setData,
  [_fetchStats.succeeded]: (state: State, response: Object) => ({
    ...state,
    stats: {
      ordersCount: get(response, 'ordersCount.ordersCount.value', 0),
      totalSales: get(response, 'totalSales.totalSales.value', 0),
      averageOrderSize: get(response, 'averageOrderSize.averageOrderSize.value', 0),
      averageOrderSum: get(response, 'averageOrderSum.averageOrderSum.value', 0),
    }
  }),
  [setName]: (state, name) => ({ ...state, name }),
  [setMainCondition]: (state, mainCondition) => ({ ...state, mainCondition }),
  [setConditions]: (state, conditions) => ({ ...state, conditions, isValid: validateConditions(conditions) }),
  [setFilterTerm]: (state, filterTerm) => ({ ...state, filterTerm }),
  [setGroupStats]: (state, stats) => ({ ...state, stats })
}, initialState);

export default reducer;
