/* @flow */

import { createReducer } from 'redux-act';
import { SubmissionError } from 'redux-form';

import createAsyncActions from './async-utils';

import api from '../lib/api';

export type Info = {
  saved?: bool,
}

type State = Info;

export const ACTION_SUBMIT = 'merchantInfoSubmit';

const { perform: submit, ...actionsSubmit } = createAsyncActions(ACTION_SUBMIT, (id, data) =>

);

const initialState: State = {};

const reducer = createReducer({
  [actionsSubmit.succeeded]: (state: State, info: Info) => ({ ...state, ...info }),
}, initialState);

const getInfo = (state: State) => state;

export {
  reducer as default,
  submit,

  /* selectors */
  getInfo,
};

