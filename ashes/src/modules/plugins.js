// @flow

import { createReducer } from 'redux-act';
import Api from 'lib/api';
import createAsyncActions from './async-utils';

export type UpdateSettingsPayload = {
  settings: {[key: string]: any},
};

export type PluginInfo = {
  createdAt: string,
  description: string,
  name: string,
  version: string,
};

export type UpdateStatePayload = {
  state: string,
};

const _fetchPlugins = createAsyncActions(
  'fetchPlugins',
  () => Api.get('/plugins')
);
export const fetchPlugins = _fetchPlugins.perform;

const _fetchSettings = createAsyncActions(
  'fetchPluginSettings',
  (name: string) => Api.get(`plugins/settings/${name}`)
);
export const fetchSettings =  _fetchSettings.perform;

const _updateSettings = createAsyncActions(
  'setPluginSettings',
  (name: string, payload: UpdateSettingsPayload) => Api.post(`plugins/settings/${name}`, payload)
);
export const updateSettings = _updateSettings.perform;

const _changeState = createAsyncActions(
  'changePluginState',
  (name: string, payload: UpdateStatePayload) => Api.patch(`plugins/settings/${name}`, payload)
);
export const changeState = _changeState.perform;

const initialState = {
  list: [],
  currentPlugin: {},
};

const reducer = createReducer({
  [_fetchPlugins.succeeded]: (state, plugins) => {
    return {
      ...state,
      list: plugins,
    };
  },
  [_fetchSettings.started]: state => {
    return {
      ...state,
      currentPlugin: {},
    };
  },
  [_fetchSettings.succeeded]: (state, currentPlugin) => {
    return {
      ...state,
      currentPlugin,
    };
  },
  [_changeState.succeeded]: (state, currentPlugin) => {
    return {
      ...state,
      currentPlugin,
    };
  },
}, initialState);

export default reducer;
