// libs
import _ from 'lodash';
import { assoc } from 'sprout-data';

// helpers
import Api from '../../../lib/api';
import * as search from '../../../lib/search';
import createStore from '../../../lib/store-creator';
import criterions, { getCriterion, getWidget } from './../../../paragons/customer-groups/criterions';
import requestAdapter from './../request-adapter';
import { aggregations } from '../../customer-groups/request';


const initialState = {
  id: null,
  type: null,
  name: null,
  mainCondition: null,
  conditions: [],
  isValid: false,
  filterTerm: null,
  isSaved: false,
  createdAt: null,
  updatedAt: null,
  stats: {
    ordersCount: 0,
    totalSales: 0,
    averageOrderSize: 0,
    averageOrderSum: 0,
  }
};

const fetchGroup = (actions, id) => dispatch => {
  return Api.get(`/groups/${id}`).then(
    (data) => {
      dispatch(actions.setData(data));
    }
  );
};

const saveGroup = actions => (dispatch, getState) => {
  const state = getState();
  const getValue = (name) => _.get(state, ['customerGroups', 'dynamic', 'group', name]);

  const id = getValue('id');
  const name = getValue('name');
  const mainCondition = getValue('mainCondition');
  const conditions = getValue('conditions');

  const data = {
    name,
    clientState: {
      mainCondition,
      conditions,
    },
    elasticRequest: requestAdapter(criterions, mainCondition, conditions).toRequest(),
  };

  //create or update
  let request;
  if (id) {
    request = Api.patch(`/groups/${id}`, data);
  } else {
    request = Api.post('/groups', data);
  }

  return request.then(
    (data) => {
      dispatch(actions.setData(data));
      dispatch(actions.setIsSaved());
    }
  );
};

const fetchGroupStats = (actions, mainCondition, conditions) => dispatch => {
  const request = requestAdapter(criterions, mainCondition, conditions);
  request.aggregations
    .add(
      new aggregations.Count('ordersCount', 'orders.referenceNumber')
    )
    .add(
      new aggregations.Sum('totalSales', 'revenue')
    )
    .add(
      new aggregations.Average('averageOrderSize', 'orders.itemsCount')
    )
    .add(
      new aggregations.Average('averageOrderSum', 'orders.grandTotal')
    );

  return search.post('customers_search_view/_search?size=0', request.toRequest()).then(
    ({aggregations}) => {
      dispatch(actions.setGroupStats({
        ordersCount: aggregations.ordersCount.ordersCount.value,
        totalSales: aggregations.totalSales.value,
        averageOrderSize: aggregations.averageOrderSize.averageOrderSize.value,
        averageOrderSum: aggregations.averageOrderSum.averageOrderSum.value,
      }));
    }
  );
};

const validateConditions = conditions => conditions.length && conditions.every(validateCondition);

const validateCondition = ([field, operator, value]) => {
  if (!field || !operator) {
    return false;
  }

  const criterion = getCriterion(field);
  const {isValid} = getWidget(criterion, operator);

  return isValid(value, criterion);
};

const reducers = {
  reset: () => {
    return initialState;
  },
  setData: (state, {id, type, name, createdAt, updatedAt, clientState: {mainCondition, conditions}}) => {
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
      isSaved: false,
    };
  },
  setName: (state, name) => {
    return {
      ...state,
      name,
    };
  },
  setMainCondition: (state, mainCondition) => {
    return {
      ...state,
      mainCondition,
    };
  },
  setConditions: (state, conditions) => {
    return {
      ...state,
      conditions,
      isValid: validateConditions(conditions),
    };
  },
  setFilterTerm: (state, filterTerm) => {
    return {
      ...state,
      filterTerm,
    };
  },
  setIsSaved: (state) => {
    return {
      ...state,
      isSaved: true,
    };
  },
  setGroupStats: (state, stats) => {
    return {
      ...state,
      stats,
    };
  }
};

const { actions, reducer } = createStore({
  path: 'customerGroups.dynamic.group',
  actions: {
    fetchGroup,
    fetchGroupStats,
    saveGroup,
  },
  reducers,
  initialState,
});

export {
  actions,
  reducer as default
};
