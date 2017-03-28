
// @class Account

import endpoints from '../endpoints';

export default class Account {
  constructor(api) {
    this.api = api;
  }

  // @method get(): Promise<LoginResponse>
  get() {
    return this.api.get(endpoints.account);
  }

  // @method update(payload: UpdateCustomerPayload): Promise<LoginResponse>
  // Updates account.
  update(payload) {
    return this.api.patch(endpoints.account, payload);
  }

  // @method changePassword(oldPassword: string, newPassword: string): Promise
  // Changes password for account.
  changePassword(oldPassword, newPassword) {
    return this.api.post(endpoints.changePassword, {
      oldPassword,
      newPassword,
    });
  }
}
