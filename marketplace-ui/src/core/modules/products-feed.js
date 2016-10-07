/* @flow */

import { createReducer } from 'redux-act';
import { SubmissionError } from 'redux-form';

import createAsyncActions from './async-utils';

import api from '../lib/api';

export type Feed = {
  id?: number;
}

type FeedResponse = {
  products_feed: Feed
}

type State = Feed;

export const ACTION_SUBMIT = 'productsFeedSubmit';
export const ACTION_UPLOAD = 'productsFeedUpload';

const { perform: submit, ...submitActions } = createAsyncActions(ACTION_SUBMIT, (id, data) =>
  new Promise((resolve, reject) =>
    api.post(`/merchants/${id}/products_feed`, { products_feed: { ...data } })
      .then(() => resolve())
      .catch(err => reject(new SubmissionError(err.response.data.errors)))
  )
);


const { perform: upload, ...uploadActions } = createAsyncActions(ACTION_UPLOAD, (id, data) =>
  new Promise((resolve, reject) =>
    api.post(`/merchants/${id}/products_feed`, { products_feed: { ...data } })
      .then(() => resolve())
      .catch(err => reject(new SubmissionError(err.response.data.errors)))
  )
);

const initialState: State = {};

const reducer = createReducer({
  [submitActions.succeeded]: (state: State, feed: FeedResponse) => ({ ...state, ...feed.products_feed }),
  [uploadActions.succeeded]: (state: State, feed: FeedResponse) => ({ ...state, ...feed.products_feed }),
}, initialState);

const getFeed = (state: State) => state;

export {
  reducer as default,
  submit,
  upload,

  /* selectors */
  getFeed,
};
