
import { useRouterHistory } from 'react-router';
import createBrowserHistory from 'history/lib/createBrowserHistory';
import { env } from 'lib/env';

const createAppHistory = useRouterHistory(createBrowserHistory);

let browserHistory = null;

if (typeof window != 'undefined') {
  browserHistory = createAppHistory({
    basename: env.URL_PREFIX,
  });
}

export {
  browserHistory,
};
