import Api from '../lib/api';
import _ from 'lodash';
import { createReducer } from 'redux-act';
import reduceReducers from 'reduce-reducers';
import createStore from '../lib/store-creator';
import createAsyncActions from './async-utils';

/**
 * Mock data
 */
function getImage() {
  const src = `http://lorempixel.com/286/286/fashion/?${_.random(1, 100)}`;
  const title = 'Lorem-ipsum.jpg';
  const du = 'Uploaded 02/02/2016 16:32';
  const album = ['Default', 'Mobile'][_.random(0, 1)];

  return { src, title, du, album };
}

const initialState = {
  list: [],
};

export default function createImagesModule(entity, scope) {
  /**
   * Actions
   */
  const fetchAsync = createAsyncActions(entity, (id) => {
    const imagesCount = _.random(1, 20);
    const images = [];

    for (let i = 0; i < imagesCount; i++) {
      images.push(getImage());
    }

    console.log(`fetching images for ${entity} with id ${id}`);

    return new Promise(resolve => {
      setTimeout(() => resolve(images), _.random(100, 1000));
    });
  });

  const fetch = (actions, entityId) => dispatch => {
    dispatch(fetchAsync.perform(entityId));
  };

  /**
   * Reducers
   */
  const asyncReducer = createReducer({
    [fetchAsync.succeeded]: (state, images) => {
      console.log(images);
      return {
        ...state,
        list: images
      };
    }
  });

  const { actions, reducer } = createStore({
    entity,
    scope,
    reducers: {},
    actions: {
      fetch,
    },
    initialState
  });

  const rootReducer = reduceReducers(asyncReducer, reducer);

  return {
    reducer: rootReducer,
    actions: {
      fetch,
      ...actions,
    }
  };
};
