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
  images: ?Array<FileInfo>;
}

export type FileInfo = {
  id: ?number;
  title: ?string;
  alt: ?string;
  src: string;
  file: File;
  loading: ?boolean;
  uploadedAt: ?string;
}

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

function actionPath(entity: string, action: string) {
  return `${entity}${_.capitalize(action)}`;
}

/**
 * Generate module for handling images for given entity
 *
 * @param {String} entity Entity name to generate images module for
 *
 * @returns {{reducer: (), actions: {}}}
 */
export default function createImagesModule(entity: string): Module {

  /** Internal async actions */

  const _uploadImages = createAsyncActions(
    actionPath(entity, 'uploadImages'),
    (context:string, albumId: string, files: FileInfo) => {
      const formData = new FormData();

      files.forEach((file: FileInfo) => {
        formData.append('upload-file', file.file);
      });

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

  /**
   * Upload images array
   *
   * @param {String} context System context
   * @param {Number} albumId Album id
   * @param {FileInfo[]} files Array if files to upload
     */
  const uploadImages = (context: string, albumId: number, files: Array<FileInfo>) => dispatch => {
    return dispatch(_uploadImages.perform(context, albumId, files));
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

  const editImage = (album: string, idx: number, imageInfo: ImageInfo) => (dispatch, getState) => {
    const album = get(getState, ['albums']).find((album: Album) => album.id === id);
    dispatch(editImageAsync.perform(album, idx, imageInfo));
  };

  const deleteImage = (context:string, albumId: number, idx: number) => (dispatch, getState) => {
    let albums = get(getState(), [entity, 'images', 'albums']);
    let album  = albums.find((album: Album) => album.id === albumId);

    album = assoc(album, ['images'], [...album.images.slice(0, idx), ...album.images.slice(idx + 1)]);

    dispatch(editAlbum(context, albumId, album));
  };

  /** Reducers */
  const asyncReducer = createReducer({
    [_uploadImages.succeeded]: (state: State, response: Album) => {
      const idx = _.findIndex(state.albums, (album: Album) => album.id === response.id);

      return assoc(state, ['albums', idx], response);
    },

    [editImageAsync.succeeded]: (state: State, [[album, idx, imageInfo]]) => {
      const path = ['list', album, idx];
      return assoc(state,
        [...path, 'title'], imageInfo.title,
        [...path, 'alt'], imageInfo.alt,
        [...path, 'inProgress'], false,
      );
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
      uploadImages,
      editImage,
      deleteImage,
      fetchAlbums,
      addAlbum,
      editAlbum,
      deleteAlbum
    }
  };
};
