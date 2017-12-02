// @flow

import _ from 'lodash';
import { createReducer } from 'redux-act';
import Api from 'lib/api';
import { assoc } from 'sprout-data';

import { createAsyncActions } from '@foxcommerce/wings';

export type UpdateSettingsPayload = {
  settings: {[key: string]: any},
};

export type PluginInfo = {
  createdAt: string,
  description: string,
  name: string,
  version: string,
};

export type SettingDef = {
  name: string,
  title: string,
  type: string,
  "default": any,
};

const _fetchPlugins = createAsyncActions(
  'fetchPlugins',
  () => Api.get('/plugins')
);
export const fetchPlugins = _fetchPlugins.perform;

const _fetchSettings = createAsyncActions(
  'fetchPluginSettings',
  (name: string) => Api.get(`plugins/settings/${name}/detailed`),
  (...args) => args
);
export const fetchSettings =  _fetchSettings.perform;

export function lazyFetchSettings(name: string) {
  const alreadyCalled = function(state) {
    return _.get(state.asyncActions, 'fetchPluginSettings.inProgress', false) ||
      _.get(state.asyncActions, 'fetchPluginSettings.finished', false);
  };
  return (dispatch: Function, getState: Function) => {
    if (!alreadyCalled(getState())) {
      dispatch(_fetchSettings.perform(name));
    }
  };
}

const _updateSettings = createAsyncActions(
  'setPluginSettings',
  (name: string, payload: UpdateSettingsPayload) => Api.post(`plugins/settings/${name}`, payload),
  (...args) => args
);
export const updateSettings = _updateSettings.perform;

const initialState = {
  list: [],
  detailed: {},
};

const reducer = createReducer({
  [_fetchPlugins.succeeded]: (state, plugins) => {
    return {
      ...state,
      list: plugins,
    };
  },
  [_fetchSettings.started]: (state, [name]) => {
    return assoc(state, ['detailed', name], {});
  },
  [_fetchSettings.succeeded]: (state, [resp, name]) => {
    return assoc(state, ['detailed', name], resp);
  },
  [_updateSettings.started]: (state, [name, payload]) => {
    return assoc(state, ['detailed', name, 'settings'], payload.settings);
  },
  [_updateSettings.succeeded]: (state, [resp, name, payload]) => {
    return assoc(state, ['detailed', name, 'settings'], payload.settings);
  }
}, initialState);

export default reducer;
