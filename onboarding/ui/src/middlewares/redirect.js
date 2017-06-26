import { LOCATION_CHANGE } from 'react-router-redux';
import isServer from '../utils/isServer';

// eslint-disable-next-line no-unused-vars
export default app => store => next => action => {
  if (!isServer) {
    return next(action);
  }

  if (action.type === LOCATION_CHANGE && action.payload.pathname && action.payload.action === 'REPLACE') {
    app.redirect(action.payload.pathname);
  }

  return next(action);
};
