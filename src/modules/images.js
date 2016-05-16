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

export type Album = {
  id: number;
  name: string;
  images: ?Array<Image>;
}

export type Image = {
  id: string;
  src: string;
  album: string;
  title: ?string;
  alt: ?string;
  du: ?string;
  inProgress: boolean;
};

export type ImageInfo = {
  title: ?string;
  alt: ?string;
}

type State = {
  list: any;
  albums: any;
}

const initialState: State = {
  list: {},
  albums: []
};

const images: Array<Image> = [];

function actionPath(entity: string, action: string) {
  return `${entity}${_.capitalize(action)}`;
}

const b64toBlob = (b64Data: string, contentType = '', sliceSize = 512): Blob => {
  const byteCharacters = atob(b64Data);
  const byteArrays = [];

  for (let offset = 0; offset < byteCharacters.length; offset += sliceSize) {
    const slice = byteCharacters.slice(offset, offset + sliceSize);

    const byteNumbers = new Array(slice.length);
    for (let i = 0; i < slice.length; i++) {
      byteNumbers[i] = slice.charCodeAt(i);
    }

    const byteArray = new Uint8Array(byteNumbers);

    byteArrays.push(byteArray);
  }

  return new Blob(byteArrays, { type: contentType });
};

/**
 * Generate module for handling images for given entity
 *
 * @param {String} entity Entity name to generate images module for
 *
 * @returns {{reducer: (), actions: {}}}
 */
export default function createImagesModule(entity: string): Module {

  /** Internal async actions */
  const fetchAsync = createAsyncActions(actionPath(entity, 'fetchImages'), (id: string|number) => {
    return new Promise(resolve => {
      setTimeout(() => resolve(images), _.random(100, 1000));
    });
  });

  const _uploadImage = createAsyncActions(
    actionPath(entity, 'uploadImage'),
    (context:string, albumId: string, file: File) => {
      const formData = new FormData();
      formData.append('title', file.file.name);
      formData.append('upload-file', file.file);

      return Api.post(`/albums/${context}/${albumId}/images`, formData);
    }
  );

  const editImageAsync = createAsyncActions(actionPath(entity, 'editImage'), (album: string, idx: number, imageInfo: ImageInfo) => {
    return new Promise(resolve => {
      setTimeout(() => resolve([album, idx, imageInfo]), _.random(100, 1000));
    });
  }, (...args) => args);

  const deleteImageAsync = createAsyncActions(actionPath(entity, 'deleteImage'), (album: string, idx: number) => {
    return new Promise(resolve => {
      setTimeout(() => resolve([album, idx]), _.random(100, 1000));
    });
  }, (...args) => args);


  const _fetchAlbums = createAsyncActions(
    actionPath(entity, 'fetchAlbums'),
    (context:string, entityId:string) => {
      return Api.get(`/${entity}/${context}/${entityId}/albums`);
    }
  );

  const _addAlbum = createAsyncActions(
    actionPath(entity, 'addAlbum'),
    (context:string, entityId:string, album:Album) => {
      return Api.post(`/${entity}/${context}/${entityId}/albums`, album );
    }
  );

  const _editAlbum = createAsyncActions(
    actionPath(entity, 'editAlbum'),
    (context:string, albumId: number, album:Album) => {
      return Api.patch(`/albums/${context}/${albumId}`, _.pick(album, ['name', 'images']));
    }
  );

  const _deleteAlbum = createAsyncActions(
    actionPath(entity, 'deleteAlbum'),
    (context:string, albumId:number) => {
      return Api.delete(`/albums/${context}/${albumId}`);
    }
  );



  /** External actions */

  const fetch = (entityId: string|number) => dispatch => {
    dispatch(fetchAsync.perform(entityId));
  };

  const uploadImage = (context: string, albumId: number, image: any) => dispatch => {
    return dispatch(_uploadImage.perform(context, albumId, image));
  };

  const editImage = (album: string, idx: number, imageInfo: ImageInfo) => dispatch => {
    dispatch(editImageAsync.perform(album, idx, imageInfo));
  };

  const deleteImage = (album: string, idx: number) => dispatch => {
    dispatch(deleteImageAsync.perform(album, idx));
  };

  /**
   * Fetch all available albums for given context/entity
   *
   * @param {String} context System context
   * @param {Number} entityId Master entity id
   *
   * @return Promise
   */
  const fetchAlbums = (context: string, entityId: number) => dispatch => {
    return dispatch(_fetchAlbums.perform(context, entityId));
  };

  /**
   * Create new album for given context/entity
   *
   * @param {String} context System context
   * @param {Number} entityId Master entity id
   * @param {Album} album Album object
   */
  const addAlbum = (context: string, entityId: string, album: Album) => dispatch => {
    return dispatch(_addAlbum.perform(context, entityId, album));
  };

  /**
   * Edit album (used w/o entity specification)
   *
   * @param {String} context System context
   * @param {Number} albumId Album id
   * @param {album} album Album object
   */
  const editAlbum = (context:string, albumId: number, album: Album) => dispatch => {
    return dispatch(_editAlbum.perform(context, albumId, album));
  };

  /**
   * Delete album (used w/o entity specification)
   *
   * @param {String} context System context
   * @param {Number} albumId Album id
   */
  const deleteAlbum = (context: string, albumId: number) => dispatch => {
    return dispatch(_deleteAlbum.perform(context, albumId));
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

    [_uploadImage.succeeded]: (state: State, response: Album) => {
      const idx = _.findIndex(state.albums, (album: Album) => album.id === response.id);

      return assoc(state, ['albums', idx], response);
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

    [_fetchAlbums.succeeded]: (state: State, response: Array<Album>) => {
      return assoc(state, ['albums'], response.sort((a: Album, b: Album) => b.id - a.id));
    },
    [_addAlbum.succeeded]: (state: State, response: Album) => {
      return assoc(state, ['albums'], [response, ...state.albums]);
    },
    [_editAlbum.succeeded]: (state: State, response: Album) => {
      const idx = _.findIndex(state.albums, (album: Album) => album.id === response.id);

      return assoc(state, ['albums', idx], response);
    },
    [_deleteAlbum.succeeded]: (state: State, response: any) => {
      return assoc(state, ['albums'], state.albums);
    },
  }, initialState);

  return {
    reducer: asyncReducer,
    actions: {
      fetch,
      uploadImage,
      editImage,
      deleteImage,
      fetchAlbums,
      addAlbum,
      editAlbum,
      deleteAlbum
    }
  };
};

const s = {
  createdAt: "2016-05-14T03:30:53.501Z",
  id: 62,
  images: [{ src: "https://s3-us-west-1.amazonaws.com/foxcomm-images/albums/1/62/okt1left.jpg" }],
  name: "First Album 4.0",
  updatedAt: "2016-05-14T03:30:53.501Z",
}
