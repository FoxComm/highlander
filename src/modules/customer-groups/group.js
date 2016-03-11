// libs
import _ from 'lodash';
import { assoc } from 'sprout-data';

// helpers
import Api from '../../lib/api';
import createStore from '../../lib/store-creator';
import criterions from './criterions';
import buildQuery from './query';

const initialState = {
  id: null,
  type: null,
  name: null,
  query: {},
  customersCount: 0,
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

  const id = _.get(state, 'customerGroups.group.id');
  const name = _.get(state, 'customerGroups.group.name');
  const query = _.get(state, 'customerGroups.group.query');

  const data = {
    name,
    clientState: query,
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
  setData: (state, {id, type, name, clientState}) => {
    return {
      ...state,
      id,
      type,
      name,
      query: clientState,
    };
  },
  setName: (state, name) => {
    return assoc(state, 'name', name);
  },
  setQuery: (state, query) => {
    return assoc(state, 'query', query);
  },
  setCustomersCount: (state, customersCount) => {
    return assoc(state, 'customersCount', customersCount);
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
