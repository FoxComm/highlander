
// @class SharedSearches
// Accessible via [sharedSearches](#foxapi-sharedsearches) property of [FoxApi](#foxapi) instance.

import endpoints from '../endpoints';

export default class SharedSearches {
  constructor(api) {
    this.api = api;
  }

  /**
   * @method list(scope: String): Promise<SharedSearch[]>
   * List shared searches.
   */
  list(scope) {
    return this.api.get(endpoints.sharedSearches(scope));
  }

  /**
   * @method create(sharedSearch: SharedSearchPayload): Promise<SharedSearch>
   * Create shared search.
   */
  create(sharedSearch) {
    return this.api.post(endpoints.sharedSearches(), sharedSearch);
  }

  /**
   * @method one(code: String): Promise<SharedSearch>
   * Find shared search by code.
   */
  one(code) {
    return this.api.get(endpoints.sharedSearch(code));
  }

  /**
   * @method delete(code: String): Promise
   * Delete shared search.
   */
  delete(code) {
    return this.api.delete(endpoints.sharedSearch(code));
  }

  /**
   * @method getAssociates(code: String): Promise<StoreAdmin[]>
   * Get shared search associates.
   */
  getAssociates(code) {
    return this.api.get(endpoints.sharedSearchAssociates(code));
  }

  /**
   * @method addAssociate(code: String, payload: SharedSearchAssociationPayload): Promise<ValidationResult<SharedSearch>>
   * Add shared search associate.
   */
  addAssociate(code, payload) {
    return this.api.post(endpoints.sharedSearchAssociate(code), payload);
  }

  /**
   * @method removeAssociate(code: String, associateId: Number): Promise
   * Remove shared search associate.
   */
  removeAssociate(code, associateId) {
    return this.api.delete(endpoints.sharedSearchAssociate(code, associateId));
  }
}
