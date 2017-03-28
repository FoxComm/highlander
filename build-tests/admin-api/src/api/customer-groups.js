
// @class CustomerGroups
// Accessible via [customerGroups](#foxapi-customergroups) property of [FoxApi](#foxapi) instance.

import endpoints from '../endpoints';

export default class CustomerGroups {
  constructor(api) {
    this.api = api;
  }

  /**
   * @method list(): Promise<GroupResponse[]>
   * List customer groups.
   */
  list() {
    return this.api.get(endpoints.customerGroups);
  }

  /**
   * @method create(group: GroupPayload): Promise<GroupResponse>
   * Create customer group.
   */
  create(group) {
    return this.api.post(endpoints.customerGroups, group);
  }

  /**
   * @method one(groupId: Number): Promise<GroupResponse>
   * Find customer group by id.
   */
  one(groupId) {
    return this.api.get(endpoints.customerGroup(groupId));
  }

  /**
   * @method update(customerId: Number, group: GroupPayload): Promise<GroupResponse>
   * Update customer group details.
   */
  update(groupId, group) {
    return this.api.patch(endpoints.customerGroup(groupId), group);
  }
}
