
import { UPDATE_LOCATION } from 'react-router-redux';

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
