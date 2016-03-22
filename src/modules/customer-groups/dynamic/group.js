// libs
import _ from 'lodash';
import { assoc } from 'sprout-data';

// helpers
import Api from '../../../lib/api';
import * as search from '../../../lib/search';
import createStore from '../../../lib/store-creator';
import criterions from './../../../paragons/customer-groups/criterions';
import queryAdapter from './../query-adapter';


const initialState = {
  id: null,
  type: null,
  name: null,
  mainCondition: null,
  conditions: [],
  filterTerm: null,
  saved: false,
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

  const query = queryAdapter(criterions, mainCondition, conditions);
  const data = {
    name,
    clientState: {
      mainCondition,
      conditions,
    },
    elasticRequest: query.toRequest(),
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
      dispatch(actions.setSaved());
    }
  );
};

const reducers = {
  reset: () => {
    return initialState;
  },
  setData: (state, {id, type, name, clientState: {mainCondition, conditions}}) => {
    return {
      ...state,
      id,
      type,
      name,
      mainCondition,
      conditions,
      saved: false,
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
    };
  },
  setFilterTerm: (state, filterTerm) => {
    return {
      ...state,
      filterTerm,
    };
  },
  setSaved: (state) => {
    return {
      ...state,
      saved: true,
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
