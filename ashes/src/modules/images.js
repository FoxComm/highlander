/* @flow */

import _ from 'lodash';
import { get, assoc } from 'sprout-data';
import { createReducer, createAction } from 'redux-act';
import { createAsyncActions } from '@foxcomm/wings';
import Api from '../lib/api';

type Module = {
  reducer: Function;
  actions: any;
};

export type NewAlbum = {
  name: string;
  images: Array<ImageFile>;
};

export type Album = NewAlbum & {
  id: number;
  failedImagesCount?: number, // UI specific stuff
};

export type FileInfo = {
  id?: number;
  src: string;
  file: File;
  loading: boolean;
  failed?: boolean;
  createdAt?: string;
};

export type ImageInfo = {
  title: string;
  src: string;
  baseUrl?: string;
  createdAt?: string;
  alt: ?string;
  key?: string;
};

export type ImageFile = FileInfo & ImageInfo;

type State = {
  albums: Array<Album>;
};

const initialState: State = {
  albums: [],
};

function actionPath(entity: string, action: string) {
  return `${entity}${_.upperFirst(action)}`;
}

export const omitAlbumFields = ['failedImagesCount'];

/**
 * Generate module for handling images for given entity
 *
 * @param {String} entity Entity name to generate images module for
 *
 * @returns {{reducer: (), actions: {}}}
 */
export default function createMediaModule(entity: string): Module {

  /** Internal actions */

  // workaround action to handle image editing/deleting progress state
  // as images actions performed through album
  const _editImageStarted = createAction(`${entity.toUpperCase()}_EDIT_IMAGE_STARTED`, (...args) => [...args]);

  const _uploadMedia = createAsyncActions(
    actionPath(entity, 'uploadMedia'),
    (context: string, albumId: string, files: Array<ImageFile>) => {
      const formData = new FormData();

      files.forEach((file: ImageFile) => {
        formData.append('upload-file', file.file);
      });

      return Api
        .post(`/albums/${context}/${albumId}/images`, formData)
        .then(response => {
          // try to associate not uploaded files with uploaded files via clien-gen keys
          const index = _.findIndex(response.images, { title: files[0].title });
          if (index != -1) {
            let filesIndex = 0;
            for (let i = index; i < response.images.length && filesIndex < files.length; i++, filesIndex++) {
              const img = response.images[i];
              if (img.title == files[filesIndex].title) {
                img.key = files[filesIndex].key;
              }
            }
          }
          return response;
        });
    },
    (...args) => [...args],
    { passthrowError: false }
  );

  const _uploadMediaByUrl = createAsyncActions(
    actionPath(entity, 'uploadMediaByUrl'),
    (context: string, albumId: string, url: string) => {
      return Api.post(`/albums/default/${albumId}/images/by-url`, { src: url });
    }
  );

  const _fetchAlbums = createAsyncActions(
    actionPath(entity, 'fetchAlbums'),
    (context: string, entityId: string) => {
      return Api.get(`/${entity}/${context}/${entityId}/albums`);
    }
  );

  const _addAlbum = createAsyncActions(
    actionPath(entity, 'addAlbum'),
    (context: string, entityId: string, album: Album) => {
      return Api.post(`/${entity}/${context}/${entityId}/albums`, album);
    }
  );

  const _editAlbum = createAsyncActions(
    actionPath(entity, 'editAlbum'),
    (context: string, albumId: number, album: Album) => {
      return Api.patch(`/albums/${context}/${albumId}`, _.pick(album, ['name', 'images']));
    }
  );

  const _archiveAlbum = createAsyncActions(
    actionPath(entity, 'archiveAlbum'),
    (context: string, albumId: number) => {
      return Api.delete(`/albums/${context}/${albumId}`);
    }
  );

  /** External actions */

  /**
   * Clear failedImagesCount for all albums
   */
  const clearFailedImagesCount = createAction(`${entity.toUpperCase()}_CLEAR_FAILED_IMAGES_COUNT`);

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
   * Accepts album name change and array of images, so images can be edited/deleted using this method.
   *
   * @param {String} context System context
   * @param {Number} albumId Album id
   * @param {album} album Album object
   */
  const editAlbum = (context: string, albumId: number, album: Album) => dispatch => {
    return dispatch(_editAlbum.perform(context, albumId, album));
  };

  /**
   * Delete album (used w/o entity specification)
   *
   * @param {String} context System context
   * @param {Number} albumId Album id
   */
  const archiveAlbum = (context: string, albumId: number) => dispatch => {
    return dispatch(_archiveAlbum.perform(context, albumId));
  };

  /**
   * Upload images array
   *
   * @param {String} context System context
   * @param {Number} albumId Album id
   * @param {ImageFile[]} files Array of image files to upload
   */
  const uploadMedia = (context: string, albumId: number, files: Array<ImageFile>) => dispatch => {
    return dispatch(_uploadMedia.perform(context, albumId, files));
  };

  const uploadMediaByUrl = _uploadMediaByUrl.perform;

  /**
   * Clear asyncState errors for both multipart and by-url upload
   */
  const clearErrors = () => dispatch => {
    dispatch(clearFailedImagesCount());
    dispatch(_uploadMedia.clearErrors());
    dispatch(_uploadMediaByUrl.clearErrors());
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
    let album = get(getState(), [entity, 'images', 'albums']).find((album: Album) => album.id === albumId);

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
  const deleteImage = (context: string, albumId: number, idx: number) => (dispatch, getState) => {
    let album = get(getState(), [entity, 'images', 'albums']).find((album: Album) => album.id === albumId);

    album = assoc(album, ['images'], [...album.images.slice(0, idx), ...album.images.slice(idx + 1)]);

    dispatch(_editImageStarted(albumId, idx));

    return dispatch(editAlbum(context, albumId, album));
  };

  /** Reducers */
  const reducer = createReducer({
    [clearFailedImagesCount]: (state) => {
      const albums = state.albums.map((album: Album) => assoc(album, 'failedImagesCount', 0));

      return assoc(state, 'albums', albums);
    },
    [_fetchAlbums.succeeded]: (state: State, response: Array<Album>) => {
      return assoc(state, ['albums'], response);
    },
    [_addAlbum.succeeded]: (state: State, response: Album) => {
      return assoc(state, ['albums'], [...state.albums, response]);
    },
    [_editAlbum.succeeded]: (state: State, response: Album) => {
      const idx = _.findIndex(state.albums, (album: Album) => album.id === response.id);

      return assoc(state, ['albums', idx], response);
    },
    [_archiveAlbum.succeeded]: (state: State) => {
      return assoc(state, ['albums'], state.albums);
    },
    [_uploadMedia.started]: (state: State, [context, albumId, images]) => {
      const idx = _.findIndex(state.albums, (album: Album) => album.id === albumId);
      const album = state.albums[idx];

      images = images.map((image: ImageFile) => assoc(image, 'loading', true));

      return assoc(state, ['albums', idx, 'images'], [...album.images, ...images]);
    },
    [_uploadMedia.succeeded]: (state: State, [response]) => {
      const idx = _.findIndex(state.albums, (album: Album) => album.id === response.id);

      return assoc(state, ['albums', idx], response);
    },
    [_uploadMedia.failed]: (state: State, [response, context, albumId, images]) => {
      const idx = _.findIndex(state.albums, (album: Album) => album.id === albumId);
      const album = state.albums[idx];
      const index = _.findIndex(album.images, { key: images[0].key });
      const rightIndex = index + images.length;
      const failedCount = rightIndex - index;

      if (index === -1) {
        return state;
      }

      const nextImages = [
        ...album.images.slice(0, index),
        ...album.images.slice(rightIndex)
      ];

      return assoc(state, ['albums', idx, 'images'], nextImages, ['albums', idx, 'failedImagesCount'], failedCount);
    },

    [_uploadMediaByUrl.succeeded]: (state: State, respAlbum: Album) => {
      const idx = _.findIndex(state.albums, (album: Album) => album.id === respAlbum.id);

      return assoc(state, ['albums', idx], respAlbum);
    },

    [_editImageStarted]: (state, [albumId, imageIndex]) => {
      const albumIndex = _.findIndex(state.albums, (album: Album) => album.id === albumId);

      return assoc(state, ['albums', albumIndex, 'images', imageIndex, 'loading'], true);
    },
  }, initialState);

  return {
    reducer,
    actions: {
      uploadMedia,
      uploadMediaByUrl,
      clearErrors,
      editImage,
      deleteImage,
      fetchAlbums,
      addAlbum,
      editAlbum,
      archiveAlbum
    }
  };
}
