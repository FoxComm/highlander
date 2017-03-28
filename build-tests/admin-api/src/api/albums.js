
// @class Albums
// Accessible via [albums](#foxapi-albums) property of [FoxApi](#foxapi) instance.

import { isBrowser } from '../utils/browser';
import endpoints from '../endpoints';

export default class Albums {
  constructor(api) {
    this.api = api;
  }

  /**
   * @method create(context: String, album: AlbumPayload): Promise<AlbumResponse>
   * Create an album.
   */
  create(context, album) {
    return this.api.post(endpoints.albums(context), album);
  }

  /**
   * @method one(context: String, albumId: Number): Promise<AlbumResponse>
   * Find album by id.
   */
  one(context, albumId) {
    return this.api.get(endpoints.album(context, albumId));
  }

  /**
   * @method update(context: String, albumId: Number, album: AlbumPayload): Promise<AlbumResponse>
   * Update album details.
   */
  update(context, albumId, album) {
    return this.api.patch(endpoints.album(context, albumId), album);
  }

  /**
   * @method archive(context: String, albumId: Number): Promise
   * Archive an album.
   */
  archive(context, albumId) {
    return this.api.delete(endpoints.album(context, albumId));
  }

  /**
   * @method uploadImages(context: String, albumId: Number, images: String[]): Promise<AlbumResponse>
   * Upload images to an album.
   */
  uploadImages(context, albumId, images) {
    if (isBrowser()) {
      throw new Error('Not implemented yet!');
    } else {
      return images
        .reduce((req, file) => req.attach('upload-file', file), this.api.agent
          .post(this.api.uri(endpoints.albumImages(context, albumId))))
        .withCredentials()
        .then(res => res.body);
    }
  }
}
