// libs
import _ from 'lodash';
import { assoc } from 'sprout-data';

// helpers
import Api from '../../lib/api';
import createStore from '../../lib/store-creator';
import criterions from './../../paragons/customer-groups/criterions';
import buildQuery from './query';

const initialState = {
  id: null,
  type: null,
  name: null,
  mainCondition: null,
  conditions: [],
};

const fetchGroup = (actions, id) => dispatch => {
  Api.get(`/groups/${id}`).then(
    (data) => {
      dispatch(actions.setData(data));
    }
  );
};

const saveGroup = actions => (dispatch, getState) => {
  const state = getState();
  const getValue = (name) => _.get(state, ['customerGroups', 'group', name]);

  const id = getValue('id');
  const name = getValue('name');
  const mainCondition = getValue('mainCondition');
  const conditions = getValue('conditions');

  //TODO build query from conditions
  const query = {};
  const data = {
    name,
    clientState: {
      mainCondition,
      conditions,
    },
    elasticRequest: buildQuery(criterions, query),
  };

  //create or update
  let request;
  if (id) {
    request = Api.patch(`/groups/${id}`, data);
  } else {
    request = Api.post('/groups', data);
  }

  request.then(
    (data) => {
      dispatch(actions.setData(data));
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
    };
  },
  setName: (state, name) => {
    return assoc(state, 'name', name);
  },
  setMainCondition: (state, condition) => {
    return assoc(state, 'mainCondition', condition);
  },
  setConditions: (state, conditions) => {
    return assoc(state, 'conditions', conditions);
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
