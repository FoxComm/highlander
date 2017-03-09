// @flow

import { createAction, createReducer } from 'redux-act';
import { assoc } from 'sprout-data';

export type LinkData = {
  title: string,
  params?: Object,
  to: string,
}

export const setRouteData = createAction('BREADCRUMBS_SET_ROUTE_DATA',
  (routeName: string, data: Object) => [routeName, data]
);

export const addExtraLinks = createAction('BREADCRUMBS_ADD_EXTRA_LINKS',
  (routeName: string, links: Array<LinkData>) => [routeName, links]
);

const initialState = {
  routesData: {},
  extraLinks: {},
};

const reducer = createReducer({
  [setRouteData]: (state, [routeName, data]) => {
    return assoc(state, ['routesData', routeName], data);
  },
  [addExtraLinks]: (state, [routeName, links]) => {
    return assoc(state, ['extraLinks', routeName], links);
  },
}, initialState);

export default reducer;
