/* @flow */

import { pick } from 'lodash';
import { createReducer } from 'redux-act';
import { SubmissionError } from 'redux-form';

import createAsyncActions from './async-utils';

import request from '../lib/request';
import api from '../lib/api';

export type Feed = {
  id?: number;
}

type State = Array<Feed>;

export const ACTION_FETCH = 'productsFeedFetch';
export const ACTION_SUBMIT = 'productsFeedSubmit';
export const ACTION_UPLOAD = 'productsFeedUpload';

const { perform: fetch, ...actionsFetch } = createAsyncActions(ACTION_FETCH, merchantId =>
  api.get(`/merchants/${merchantId}/products_feed`)
);

const { perform: submit, ...submitActions } = createAsyncActions(ACTION_SUBMIT, (merchantId, data) => {
  const schedule = data.schedule.toLowerCase() === 'weekly' ? data.scheduleDays : data.schedule;
  const feed = pick({
    ...data,
    schedule: schedule.toLowerCase(),
  }, ['name', 'url', 'schedule']);

  return new Promise((resolve, reject) =>
    api.post(`/merchants/${merchantId}/products_feed`, { products_feed: { ...feed } })
      .then((feed: Feed) => resolve(feed))
      .catch(err => reject(new SubmissionError(err.response.data.errors)))
  );
});

const { perform: upload, ...uploadActions } = createAsyncActions(ACTION_UPLOAD, (merchantId, data) => {
  const file = data.file[0];
  const type = file.type;
  const name = file.name;

  return new Promise((resolve, reject) =>
    request('get', '/s3sign', { name, type })
      .then(signed => {
        request('put', signed.signedRequest, file, { headers: { 'Content-Type': type } })
          .then(() =>
            api.post(`/merchants/${merchantId}/products_upload`, { products_upload: { file_url: signed.url } })
              .then(upload => resolve(upload))
              .catch(err => reject(new SubmissionError(err.response.data.errors)))
          )
          .catch(() => reject(new SubmissionError()));
      })
      .catch(() => reject(new SubmissionError()))
  );
});


const initialState: State = {};

const reducer = createReducer({
  [actionsFetch.succeeded]: (state: State, feed: Feed) => feed.product_feeds,
  [submitActions.succeeded]: (state: State, feed: Feed) => feed,
  [uploadActions.succeeded]: (state: State, feed: Feed) => ({ ...state, ...feed }),
}, initialState);

const getFeed = (state: State): Array<Feed> => state;

export {
  reducer as default,
  fetch,
  submit,
  upload,

  /* selectors */
  getFeed,
};
