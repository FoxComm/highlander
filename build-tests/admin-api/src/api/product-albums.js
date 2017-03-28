
// @class ProductAlbums
// Accessible via [productAlbums](#foxapi-productalbums) property of [FoxApi](#foxapi) instance.

import endpoints from '../endpoints';

export default class ProductAlbums {
  constructor(api) {
    this.api = api;
  }

  /**
   * @method list(context: String, productId: Number): Promise<AlbumResponse[]>
   * List product albums.
   */
  list(context, productId) {
    return this.api.get(endpoints.productAlbums(context, productId));
  }

  /**
   * @method create(context: String, productId: Number, album: AlbumPayload): Promise<AlbumResponse>
   * Create new product album.
   */
  create(context, productId, album) {
    return this.api.post(endpoints.productAlbums(context, productId), album);
  }

  /**
   * @method updatePosition(context: String, productId: Number, payload: UpdateAlbumPositionPayload): Promise<AlbumResponse>
   * Create new product.
   */
  updatePosition(context, productId, payload) {
    return this.api.post(endpoints.productAlbumPosition(context, productId), payload);
  }
}
