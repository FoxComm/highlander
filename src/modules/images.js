/* @flow */

import Api from '../lib/api';
import _ from 'lodash';
import { get, assoc, dissoc } from 'sprout-data';
import { createReducer } from 'redux-act';
import reduceReducers from 'reduce-reducers';
import createStore from '../lib/store-creator';
import createAsyncActions from './async-utils';

type Module = {
  reducer: Function;
  actions: any;
}

export type Image = {
  id: string;
  src: string;
  album: string;
  title?: string;
  alt?: string;
  du?: string;
  inProgress: boolean;
};

export type ImageInfo = {
  title?: string;
  alt?: string;
}

type State = {
  list: any;
  albums: any;
}

/**
 * Mock data
 */
function getImage(): Image {
  const id = _.random(0, 10000);
  const src = `http://lorempixel.com/286/286/fashion/?${id}`;
  const title = 'Lorem-ipsum.jpg';
  const alt = 'Alt image text';
  const du = 'Uploaded 02/02/2016 16: 32';
  const album = ['Default', 'Mobile'][_.random(0, 1)];

  return { id, src, title, du, album, alt, inProgress: false };
}

const initialState: State = {
  list: {},
};

const imagesCount: number = _.random(1, 20);
const images: Array<Image> = [];

for (let i = 0; i < imagesCount; i++) {
  images.push(getImage());
}

function actionPath(entity: string, action: string) {
  return `${entity}${_.capitalize(action)}`;
}

export default function createImagesModule(entity: string, scope: string): Module {

  entity = entity.toUpperCase();

  /** Async actions */
  const fetchAsync = createAsyncActions(actionPath(scope, 'fetchImages'), (id: string|number) => {
    return new Promise(resolve => {
      setTimeout(() => resolve(images), _.random(100, 1000));
    });
  });

  const editImageAsync = createAsyncActions(actionPath(scope, 'editImage'), (album: string, idx: number, imageInfo: ImageInfo) => {
    return new Promise(resolve => {
      setTimeout(() => resolve([album, idx, imageInfo]), _.random(100, 1000));
    });
  }, (...args) => args);

  const deleteImageAsync = createAsyncActions(actionPath(scope, 'deleteImage'), (album: string, idx: number) => {
    return new Promise(resolve => {
      setTimeout(() => resolve([album, idx]), _.random(100, 1000));
    });
  }, (...args) => args);

  const editAlbumAsync = createAsyncActions(actionPath(scope, 'editAlbum'), (albumName: string, newName: string) => {
    return new Promise(resolve => {
      setTimeout(() => resolve([albumName, newName]), _.random(100, 1000));
    });
  }, (...args) => args);

  const deleteAlbumAsync = createAsyncActions(actionPath(scope, 'deleteAlbum'), (album: string) => {
    return new Promise(resolve => {
      setTimeout(() => resolve(album), _.random(100, 1000));
    });
  }, (...args) => args);

  /** External actions */
  const fetch = (entityId: string|number) => dispatch => {
    dispatch(fetchAsync.perform(entityId));
  };

  const editImage = (album: string, idx: number, imageInfo: ImageInfo) => dispatch => {
    dispatch(editImageAsync.perform(album, idx, imageInfo));
  };

  const deleteImage = (album: string, idx: number) => dispatch => {
    dispatch(deleteImageAsync.perform(album, idx));
  };

  const editAlbum = (albumName: string, newName: string) => dispatch => {
    dispatch(editAlbumAsync.perform(albumName, newName));
  };

  const deleteAlbum = (album: string) => dispatch => {
    dispatch(deleteAlbumAsync.perform(album));
  };

  /** Reducers */
  const asyncReducer = createReducer({
    [fetchAsync.succeeded]: (state: State, images: Array<Image>) => {
      const byAlbum = _.groupBy(images, 'album');

      return assoc(state,
        'list', byAlbum,
        'albums', _.mapValues(byAlbum, (_, album: string) => ({ id: album })),
      );
    },

    [editImageAsync.started]: (state: State, [album, idx]) => {
      return assoc(state, ['list', album, idx, 'inProgress'], true);
    },
    [editImageAsync.failed]: (state: State, [err, [album, idx]]) => {
      return assoc(state, ['list', album, idx, 'inProgress'], false);
    },
    [editImageAsync.succeeded]: (state: State, [[album, idx, imageInfo]]) => {
      const path = ['list', album, idx];
      return assoc(state,
        [...path, 'title'], imageInfo.title,
        [...path, 'alt'], imageInfo.alt,
        [...path, 'inProgress'], false,
      );
    },

    [deleteImageAsync.started]: (state: State, [album, idx]) => {
      return assoc(state, ['list', album, idx, 'inProgress'], true);
    },
    [deleteImageAsync.failed]: (state: State, [err, [album, idx]]) => {
      return assoc(state, ['list', album, idx, 'inProgress'], false);
    },
    [deleteImageAsync.succeeded]: (state: State, [[album, idx]]) => {
      const path = ['list', album];
      const images = get(state, path, []);

      return assoc(state, path, [...images.slice(0, idx), ...images.slice(idx + 1)]);
    },

    [editAlbumAsync.started]: (state: State, [albumName]) => {
      return assoc(state, ['albums', albumName, 'inProgress'], true);
    },
    [editAlbumAsync.failed]: (state: State, [err, [albumName]]) => {
      return assoc(state, ['albums', albumName, 'inProgress'], false);
    },
    [editAlbumAsync.succeeded]: (state: State, [[albumName, newName]]) => {
      const albums = _.mapKeys(get(state, ['albums'], {}), (album, name: string) => {
        return name === albumName ? newName : name;
      });
      const lists = _.mapKeys(_.mapValues(get(state, ['list'], {}), (list, name) => {
        if (name === albumName) {
          return list.map((image: Image) => {
            image.album = newName;

            return image;
          });
        }

        return list;
      }), (list, name: string) => {
        return name === albumName ? newName : name;
      });

      state = dissoc(state,
        ['list', albumName],
      );

      return assoc(state,
        ['albums'], albums,
        ['list'], lists,
      );
    },

    [deleteAlbumAsync.started]: (state: State, [album]) => {
      return assoc(state, ['albums', album, 'inProgress'], true);
    },
    [deleteAlbumAsync.failed]: (state: State, [err, [album]]) => {
      return assoc(state, ['albums', album, 'inProgress'], false);
    },
    [deleteAlbumAsync.succeeded]: (state: State, [album]) => {
      return dissoc(state,
        ['list', album],
        ['albums', album],
      );
    },
  }, initialState);

  return {
    reducer: asyncReducer,
    actions: {
      fetch,
      editImage,
      deleteImage,
      editAlbum,
      deleteAlbum
    }
  };
};
