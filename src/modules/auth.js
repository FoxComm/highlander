
/* @flow weak */

import _ from 'lodash';
import { UPDATE_LOCATION, routeActions } from 'react-router-redux';

export function switchStage(newStage: ?string) {
  return (dispatch, getState) => {
    const stage: string = newStage || _.get(getState(), 'auth.stage') == 'login' ? 'signup' : 'login';
    dispatch(routeActions.push(`/${stage}`));
  };
}

const reducer = (state = {}, {type, payload}) => {
  if (type === UPDATE_LOCATION) {
    if (payload.pathname == '/login') {
      return {
        ...state,
        stage: 'login',
      };
    } else if (payload.pathname == '/signup') {
      return {
        ...state,
        stage: 'signup',
      };
    }
  }
  return state;
};

export default reducer;
