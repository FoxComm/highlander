// @flow

// helpers
import createStore from '../../lib/store-creator';

// data
import { reducers } from '../bulk';

const openNewWindow = () => dispatch => {
  dispatch(actions.bulkRequest());
  dispatch(actions.bulkDone());
};

const moveToAlbum = () => dispatch => {
  dispatch(actions.bulkRequest());
  dispatch(actions.bulkDone());
};

const deleteImages = () => dispatch => {
  dispatch(actions.bulkRequest());
  dispatch(actions.bulkDone());
};

const { actions, reducer } = createStore({
  path: 'products.imagesBulk',
  actions: {
    openNewWindow,
    moveToAlbum,
    deleteImages,
  },
  reducers,
});

export {
  actions,
  reducer as default
};
