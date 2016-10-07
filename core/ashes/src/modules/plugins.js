// @flow
import { createReducer } from 'redux-act';
import Api from 'lib/api';
import createAsyncActions from './async-utils';

export type UpdateSettingsPayload = {
  settings: {[key: string]: any},
}

export type PluginInfo = {
  createdAt: string,
  description: string,
  name: string,
  version: string,
}

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

const initialState = {
  list: [],
  settings: {},
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
      settings: {},
    };
  },
  [_fetchSettings.succeeded]: (state, settings) => {
    return {
      ...state,
      settings,
    };
  }
}, initialState);

export default reducer;
