// libs
import _ from 'lodash';
import { assoc } from 'sprout-data';

// helpers
import Api from '../../../lib/api';
import * as search from '../../../lib/search';
import createStore from '../../../lib/store-creator';
import criterions, { getCriterion, getWidget } from './../../../paragons/customer-groups/criterions';
import requestAdapter from './../request-adapter';


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
};

const { actions, reducer } = createStore({
  entity: 'customer-groups',
  actions: {
    fetchGroup,
    saveGroup,
  },
  reducers,
  initialState,
});

export {
  actions,
  reducer as default
};
