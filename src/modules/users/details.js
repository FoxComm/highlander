import Api from '../../lib/api';
import { createAction, createReducer } from 'redux-act';
import { haveType } from '../state-helpers';
import { assoc } from 'sprout-data';

const receiveUser = createAction('USER_RECEIVE', (id, user) => [id, user]);
const failUser = createAction('USER_FAIL', (id, err, source) => [id, err, source]);
const requestUser = createAction('USER_REQUEST');
const updateUser = createAction('USER_UPDATED', (id, user) => [id, user]);

export function fetchUser(id) {
  return dispatch => {
    dispatch(requestUser(id));
    Api.get(`/store-admins/${id}`)
      .then(
        user => dispatch(receiveUser(id, user)),
        err => dispatch(failUser(id, err, fetchUser))
      );
  };
}

const reducer = createReducer({
  [requestUser]: (entries, id) => {
    return assoc(entries,
      [id, 'isFetching'], true,
      [id, 'failed'], null
    );
  },
  [receiveUser]: (state, [id, details]) => {
    return assoc(state,
      [id, 'failed'], null,
      [id, 'isFetching'], false,
      [id, 'details'], haveType(details, 'user')
    );
  },
  [failUser]: (state, [id, err, source]) => {
    console.error(err);

    return assoc(state,
      [id, 'failed'], true,
      [id, 'isFetching'], false
    );
  },
  [updateUser]: (state, [id, details]) => {
    return assoc(state,
      [id, 'details'], details,
      [id, 'failed'], null
    );
  },
}, {});

export default reducer;
