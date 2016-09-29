import { LOCATION_CHANGE } from 'react-router-redux';
import { AC } from 'history';

const isServer = typeof self == 'undefined';

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
