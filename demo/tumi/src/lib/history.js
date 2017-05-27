
import { useRouterHistory } from 'react-router';
import createBrowserHistory from 'history/lib/createBrowserHistory';
import useNamedRoutes from 'use-named-routes';
import { env } from 'lib/env';

const createHistory = useNamedRoutes(useRouterHistory(createBrowserHistory));

let browserHistory = null;

function createAppHistory(options) {
  browserHistory = createHistory({
    basename: env.URL_PREFIX,
    ...options,
  });

  return browserHistory;
}

module.exports = {
  get browserHistory() {
    return browserHistory;
  },
  createAppHistory,
};
