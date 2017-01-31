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

const statsPeriodsMapping = {
  day: [{ 'from': 'now-1d/d' }],
  week: [{ 'from': 'now-1w/d' }],
  month: [{ 'from': 'now-1M/d' }],
  quarter: [{ 'from': 'now-3M/d' }],
  year: [{ 'from': 'now-1y/d' }],
  overall: [{ 'to': 'now' }],
};

const initialState = {
  id: null,
  groupType: null,
  name: null,
  conditions: [],
  mainCondition: null,
  elasticRequest: {},
  isValid: false,
  customersCount: 0,
  createdAt: null,
  updatedAt: null,
  stats: {
    day: null,
    week: null,
    month: null,
    quarter: null,
    year: null,
    overall: null,
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
export const setType = createAction('CUSTOMER_GROUP_SET_TYPE');
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

  const groupType = getValue('groupType');
  const groupId = getValue('id');
  const name = getValue('name');
  const mainCondition = getValue('mainCondition');
  const conditions = getValue('conditions');
  const elasticRequest = requestAdapter(groupId, criterions, mainCondition, conditions).toRequest();

  const data = {
    name,
    groupType,
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

  Object.keys(statsPeriodsMapping).forEach((period: string) => {
    request.aggregations
      .add(
        new aggregations.DateRange(period, 'orders.placedAt', statsPeriodsMapping[period])
          .add(new aggregations.Stats('sales', 'orders.subTotal'))
          .add(new aggregations.Stats('items', 'orders.itemsCount'))
      );
  });

  dispatch(_fetchStats.perform(request.toRequest()));
};

const validateConditions = (type, conditions) => {
  if (type == 'manual') return true;

  return conditions &&
    conditions.length && conditions.every(validateCondition);
};

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

const setData = (state: State, { clientState: { mainCondition, conditions }, groupType, ...rest }) => {
  return {
    ...rest,
    groupType,
    conditions,
    mainCondition,
    isValid: validateConditions(groupType, conditions),
    stats: initialState.stats,
  };
};


/*
 * Aggregations response example
 *
 * "aggregations": {
 *    "day": {
 *      "doc_count": 3,
 *        "day": {
 *          "buckets": [{
 *            "from_as_string": "2017-01-30T00:00:00.000+0000",
 *            "doc_count": 3,
 *            "from": 1485734400000,
 *            "key": "2017-01-30T00:00:00.000+0000-*",
 *            "items": { "max": 3, "sum": 5, "count": 3, "min": 1, "avg": 1.6666666666667 },
 *            "sales": { "max": 28600, "sum": 31600, "count": 3, "min": 1500, "avg": 10533.333333333 }
 *          }]
 *        }
 *    },
 *    "month": {
 *      ...
 *    }
 * }
 */
const setStats = (aggregations: Object) => {
  return Object.keys(statsPeriodsMapping).reduce((stats: Object, period: string) => {
    stats[period] = {
      ordersCount: get(aggregations, [period, period, 'buckets', 0, 'doc_count']),
      totalSales: get(aggregations, [period, period, 'buckets', 0, 'sales', 'sum']),
      averageOrderSize: get(aggregations, [period, period, 'buckets', 0, 'items', 'avg']),
      averageOrderSum: get(aggregations, [period, period, 'buckets', 0, 'sales', 'avg']),
    };

    return stats;
  }, {});
};

const reducer = createReducer({
  [reset]: (state: State) => initialState,
  [_fetchGroup.succeeded]: setData,
  [_saveGroup.succeeded]: setData,
  [_fetchStats.succeeded]: (state: State, { aggregations }: Object) => ({
    ...state,
    stats: setStats(aggregations),
  }),
  [setName]: (state, name) => ({ ...state, name }),
  [setType]: (state, groupType) => ({ ...state, groupType, isValid: validateConditions(groupType, state.conditions) }),
  [setMainCondition]: (state, mainCondition) => ({ ...state, mainCondition }),
  [setConditions]: (state, conditions) => ({ ...state, conditions, isValid: validateConditions(state.groupType, conditions) }),
  [setGroupStats]: (state, stats) => ({ ...state, stats }),
}, initialState);

export default reducer;
