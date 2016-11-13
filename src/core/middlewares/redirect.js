import { UPDATE_LOCATION } from 'react-router-redux';

const isServer = typeof self === 'undefined';

export default app => () => next => action => {
  if (!isServer || !app) {
    return next(action);
  }

  if (action.type === UPDATE_LOCATION && action.payload.pathname && action.payload.action === 'REPLACE') {
    return app.redirect(action.payload.pathname);
  }

  return next(action);
};
