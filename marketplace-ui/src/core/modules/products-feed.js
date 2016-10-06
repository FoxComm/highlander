/* @flow */

import { createReducer } from 'redux-act';
import { SubmissionError } from 'redux-form';

import createAsyncActions from './async-utils';

import api from '../lib/api';

export type Feed = {
  id?: number;
}

type FeedResponse = {
  legal_profile: Feed
}

type State = Feed;

export const ACTION_SUBMIT = 'productsFeedSubmit';

const { perform, ...actions } = createAsyncActions(ACTION_SUBMIT, (id, data) =>
  new Promise((resolve, reject) =>
    api.post(`/merchants/${id}/products_feed`, { products_feed: { ...data } })
      .then(() => resolve())
      .catch(err => reject(new SubmissionError(err.response.data.errors)))
  )
);

const initialState: State = {};

const reducer = createReducer({
  [actions.succeeded]: (state: State, feed: FeedResponse) => ({ ...state, ...feed.products_feed }),
}, initialState);

const getFeed = (state: State) => state;

export {
  reducer as default,
  perform as submit,

  /* selectors */
  getFeed,
};
