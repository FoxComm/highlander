/* @flow */

import _ from 'lodash';
import { get, assoc } from 'sprout-data';
import { createReducer, createAction } from 'redux-act';
import createAsyncActions from './async-utils';
import Api from '../lib/api';

type Module = {
  reducer: Function;
  actions: any;
}

export type TAlbum = {
  id?: number;
  name: string;
  images: Array<ImageFile>;
}

export type FileInfo = {
  id?: number|string;
  src: string;
  file: File;
  loading: boolean;
  uploadedAt?: string;
}

export type ImageInfo = {
  title: string;
  alt: ?string;
}

export type ImageFile = FileInfo & ImageInfo;

type State = {
  albums: Array<TAlbum>;
}

const initialState: State = {
  albums: [],
};

// @TODO: get rid of this hack - backend should provide `id` property itself
function resolveImageId(knownHash, image) {
  if (!image.id) {
    knownHash[image.src] = image.src in knownHash ? knownHash[image.src] + 1 : 1;
    return `${image.src}_${knownHash[image.src]}`;
  }
  return image.id;
}

function insertIds(images) {
  const hash = {};
  images.map(image => {
    if (!image.id) {
      image.id = resolveImageId(hash, image);
    }
  });

  return images;
}

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

  /** Internal actions */

  // workaround action to handle image editing/deleting progress state
  // as images actions performed through album
  const _editImageStarted = createAction(`${entity.toUpperCase()}_EDIT_IMAGE_STARTED`, (...args) => [...args]);

  const _uploadImages = createAsyncActions(
    actionPath(entity, 'uploadImages'),
    (context:string, albumId: string, files: Array<ImageFile>) => {
      const formData = new FormData();

      files.forEach((file: ImageFile) => {
        formData.append('upload-file', file.file);
      });

      return Api.post(`/albums/${context}/${albumId}/images`, formData);
    },
    (...args) => [...args]
  );

  const _fetchAlbums = createAsyncActions(
    actionPath(entity, 'fetchAlbums'),
    (context:string, entityId:string) => {
      return Api.get(`/${entity}/${context}/${entityId}/albums`);
    }
  );

  const _addAlbum = createAsyncActions(
    actionPath(entity, 'addAlbum'),
    (context:string, entityId:string, album:TAlbum) => {
      return Api.post(`/${entity}/${context}/${entityId}/albums`, album );
    }
  );

  const _editAlbum = createAsyncActions(
    actionPath(entity, 'editAlbum'),
    (context:string, albumId: number, album:TAlbum) => {
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
   * @param {TAlbum} album Album object
   */
  const addAlbum = (context: string, entityId: string, album: TAlbum) => dispatch => {
    return dispatch(_addAlbum.perform(context, entityId, album));
  };

  /**
   * Edit album (used w/o entity specification)
   * Accepts album name change and array of images, so images can be edited/deleted using this method.
   *
   * @param {String} context System context
   * @param {Number} albumId Album id
   * @param {album} album Album object
   */
  const editAlbum = (context:string, albumId: number, album: TAlbum) => dispatch => {
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

  /**
   * Upload images array
   *
   * @param {String} context System context
   * @param {Number} albumId Album id
   * @param {ImageFile[]} files Array of image files to upload
   */
  const uploadImages = (context: string, albumId: number, files: Array<ImageFile>) => dispatch => {
    return dispatch(_uploadImages.perform(context, albumId, files));
  };

  /**
   * Edit image info
   *
   * @param {String} context System context
   * @param {Number} albumId Album id
   * @param {Number} idx Image index to delete
   * @param {ImageFile} image Updated image object
     */
  const editImage = (context: string, albumId: number, idx: number, image: ImageFile) => (dispatch, getState) => {
    let album = get(getState(), [entity, 'images', 'albums']).find((album: TAlbum) => album.id === albumId);

    album = assoc(album, ['images', idx], image);

    dispatch(_editImageStarted(albumId, idx));

    return dispatch(editAlbum(context, albumId, album));
  };

  /**
   * Delete image from album
   *
   * @param {String} context System context
   * @param {Number} albumId Album id
   * @param {Number} idx Image index to delete
     */
  const deleteImage = (context:string, albumId: number, idx: number) => (dispatch, getState) => {
    let album = get(getState(), [entity, 'images', 'albums']).find((album: TAlbum) => album.id === albumId);

    album = assoc(album, ['images'], [...album.images.slice(0, idx), ...album.images.slice(idx + 1)]);

    dispatch(_editImageStarted(albumId, idx));

    return dispatch(editAlbum(context, albumId, album));
  };

  /** Reducers */
  const asyncReducer = createReducer({
    [_fetchAlbums.succeeded]: (state: State, response: Array<TAlbum>) => {
      _.each(response, (album: TAlbum) => {
        album.images = insertIds(album.images);
      });
      return assoc(state,
        ['albums'],
        response.sort((a: TAlbum, b: TAlbum) => Number(b.id) - Number(a.id)),
      );
    },
    [_addAlbum.succeeded]: (state: State, response: TAlbum) => {
      response.images = insertIds(response.images);

      return assoc(state, ['albums'], [response, ...state.albums]);
    },
    [_editAlbum.succeeded]: (state: State, response: TAlbum) => {
      const idx = _.findIndex(state.albums, (album: TAlbum) => album.id === response.id);
      response.images = insertIds(response.images);

      return assoc(state, ['albums', idx], response);
    },
    [_deleteAlbum.succeeded]: (state: State, response: any) => {
      return assoc(state, ['albums'], state.albums);
    },
    [_uploadImages.started]: (state: State, [context, albumId, images]) => {
      const idx = _.findIndex(state.albums, (album: TAlbum) => album.id === albumId);
      const album = get(state, ['albums', idx]);

      images = images.map((image: ImageFile) => assoc(image, 'loading', true));

      return assoc(state, ['albums', idx, 'images'], insertIds([...album.images, ...images]));
    },
    [_uploadImages.succeeded]: (state: State, [response]) => {
      const idx = _.findIndex(state.albums, (album: TAlbum) => album.id === response.id);
      response.images = insertIds(response.images);

      return assoc(state, ['albums', idx], response);
    },

    [_editImageStarted]: (state, [albumId, imageIndex]) => {
      const albumIndex = _.findIndex(state.albums, (album: TAlbum) => album.id === albumId);

      return assoc(state, ['albums', albumIndex, 'images', imageIndex, 'loading'], true);
    }
  }, initialState);

  return {
    reducer: asyncReducer,
    actions: {
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
