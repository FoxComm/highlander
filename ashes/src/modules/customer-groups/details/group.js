/* @flow */

// libs
import get from 'lodash/get';
import { createReducer, createAction } from 'redux-act';

// helpers
import Api from 'lib/api';
import * as search from 'lib/search';
import { post } from 'lib/search';
import criterions, { getCriterion, getWidget } from 'paragons/customer-groups/criterions';
import { Request, aggregations } from 'elastic/request';
import { createAsyncActions } from '@foxcomm/wings';

import requestAdapter from '../utils/request-adapter';

const mapping = 'customers_search_view';

const initialState = {
  id: null,
  type: null,
  name: null,
  conditions: [],
  mainCondition: null,
  elasticRequest: {},
  isValid: false,
  customersCount: 0,
  createdAt: null,
  updatedAt: null,
  stats: {
    ordersCount: null,
    totalSales: null,
    averageOrderSize: null,
    averageOrderSum: null,
  }
};

/**
 * Internal actions
 */
const _fetchGroup = createAsyncActions('fetchCustomerGroup', (groupId: number) => Api.get(`/customer-groups/${groupId}`));

const _saveGroup = createAsyncActions(
  'saveCustomerGroup',
  (groupId, data) => {
    let request;
    if (groupId) {
      request = Api.patch(`/customer-groups/${groupId}`, data);
    } else {
      request = Api.post('/customer-groups', data);
    }

    return request;
  }
);

const _archiveGroup = createAsyncActions('archiveCustomerGroup', (groupId: number) => Api.delete(`/customer-groups/${groupId}`));

const _fetchStats = createAsyncActions('fetchStatsCustomerGroup', request =>
  search.post(`${mapping}/_search?size=0`, request)
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
  const elasticRequest = requestAdapter(groupId, criterions, mainCondition, conditions).toRequest();

  const data = {
    name,
    clientState: {
      mainCondition,
      conditions,
    },
    elasticRequest,
  };

  return dispatch(_saveGroup.perform(groupId, data));
};

/**
 * Save new group from predefined template
 *
 * @param {TTemplate} template
 *
 * @return Promise
 */
export const saveGroupFromTemplate = (template: TTemplate) => (dispatch: Function, getState: Function) =>
  dispatch(_saveGroup.perform(void 0, template));

/**
 * Fetch customer group's stats
 *
 * @return Promise
 */
export const fetchGroupStats = () => (dispatch: Function, getState: Function) => {
  const state = getState();
  const group = get(state, ['customerGroups', 'details', 'group']);

  const request = requestAdapter(group.id, criterions, group.mainCondition, group.conditions);

  request.aggregations
    .add(new aggregations.Sum('ordersCount', 'orderCount'))
    .add(new aggregations.Sum('totalSales', 'orders.subTotal'))
    .add(new aggregations.Average('averageOrderSize', 'orders.itemsCount'))
    .add(new aggregations.Average('averageOrderSum', 'orders.subTotal'));

  dispatch(_fetchStats.perform(request.toRequest()));
};

const validateConditions = conditions =>
conditions && conditions.length && conditions.every(validateCondition);

const validateCondition = ([field, operator, value]) => {
  if (!field || !operator) {
    return false;
  }

  const criterion = getCriterion(field);
  const { isValid } = getWidget(criterion, operator);

  return isValid(value, criterion);
};

type State = {
  group: TCustomerGroup,
};

const setData = (state: State, { clientState: { mainCondition, conditions }, ...rest }) => {
  return {
    ...rest,
    conditions,
    mainCondition,
    isValid: validateConditions(conditions),
    stats: initialState.stats,
  };
};

const reducer = createReducer({
  [reset]: (state: State) => initialState,
  [_fetchGroup.succeeded]: setData,
  [_saveGroup.succeeded]: setData,
  [_fetchStats.succeeded]: (state: State, { aggregations }: Object) => ({
    ...state,
    stats: {
      ordersCount: get(aggregations, 'ordersCount.value'),
      totalSales: get(aggregations, 'totalSales.totalSales.value'),
      averageOrderSize: get(aggregations, 'averageOrderSize.averageOrderSize.value'),
      averageOrderSum: get(aggregations, 'averageOrderSum.averageOrderSum.value'),
    }
  }),
  [setName]: (state, name) => ({ ...state, name }),
  [setMainCondition]: (state, mainCondition) => ({ ...state, mainCondition }),
  [setConditions]: (state, conditions) => ({ ...state, conditions, isValid: validateConditions(conditions) }),
  [setGroupStats]: (state, stats) => ({ ...state, stats }),
}, initialState);

export default reducer;
