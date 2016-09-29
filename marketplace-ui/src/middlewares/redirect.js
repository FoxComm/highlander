import { LOCATION_CHANGE } from 'react-router-redux';
import isServer from '../utils/isServer';

export default app => store => next => action => {
  if (!isServer) {
    return next(action);
  }

  if (action.type === LOCATION_CHANGE && action.payload.pathname && action.payload.action === 'REPLACE') {
    console.info('redirecting', action.payload.pathname);
    app.redirect(action.payload.pathname);
  }

  return next(action);
};
