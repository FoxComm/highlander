// libs
import _ from 'lodash';
import { assoc } from 'sprout-data';

// helpers
import Api from '../../lib/api';
import createStore from '../../lib/store-creator';
import criterions from './../../paragons/customer-groups/criterions';
import queryAdapter from './query-adapter';
import buildQuery from './query';

const initialState = {
  id: null,
  type: null,
  name: null,
  mainCondition: null,
  conditions: [],
  saved: false,
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

  const query = queryAdapter(mainCondition, conditions);
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
