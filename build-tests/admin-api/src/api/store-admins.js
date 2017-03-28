
// @class StoreAdmins
// Accessible via [storeAdmins](#foxapi-storeadmins) property of [FoxApi](#foxapi) instance.

import endpoints from '../endpoints';

export default class StoreAdmins {
  constructor(api) {
    this.api = api;
  }

  /**
   * @method create(storeAdmin: StoreAdminCreatePayload): Promise<StoreAdmin>
   * Create new store admin.
   */
  create(storeAdmin) {
    return this.api.post(endpoints.storeAdmins, storeAdmin);
  }

  /**
   * @method one(storeAdminId: Number): Promise<StoreAdmin>
   * Find store admin by id.
   */
  one(storeAdminId) {
    return this.api.get(endpoints.storeAdmin(storeAdminId));
  }

  /**
   * @method update(storeAdminId: Number, storeAdmin: StoreAdminUpdatePayload): Promise<StoreAdmin>
   * Update store admin details.
   */
  update(storeAdminId, storeAdmin) {
    return this.api.patch(endpoints.storeAdmin(storeAdminId), storeAdmin);
  }
}
