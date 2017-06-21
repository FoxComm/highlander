import StandardCustomerApi from '@foxcomm/api-js';
import StandardAdminApi from '@foxcomm/admin-api-js';
import superagent from 'superagent';
import cookie from 'cookie';
import config from '../config';
import $ from '../payloads';
import wrapWithApiLogging from './wrapWithApiLogging';

const endpoints = {
  esStoreAdmins: 'search/admin/store_admins_search_view/_search',
};

const constructApiOptions = () => ({
  api_url: `${config.apiUrl}/api`,
  stripe_key: config.stripeKey,
  agent: superagent.agent(),
});

export class AdminApi extends StandardAdminApi {
  constructor(testContext) {
    super(constructApiOptions());
    this.testContext = testContext;
    this.monkeypatch();
    if (testContext) {
      wrapWithApiLogging(this);
    }
  }
  monkeypatch() {
    this.storeAdmins.list = (maxCount = 50) => {
      const url = `${config.apiUrl}/api/${endpoints.esStoreAdmins}`;
      const request = this.agent.post(url).withCredentials();
      const cookies = cookie.parse(request.cookies);
      return request
        .query({ size: maxCount })
        .set('JWT', cookies.JWT)
        .send({ query: { bool: {} }, sort: [{ createdAt: { order: 'desc' } }] })
        .then(res => res.body.result);
    };
  }
  static async loggedIn(testContext) {
    const api = new AdminApi(testContext);
    await api.auth.login($.adminEmail, $.adminPassword, $.adminOrg);
    return api;
  }
}

export class CustomerApi extends StandardCustomerApi {
  constructor(testContext) {
    super(constructApiOptions());
    this.testContext = testContext;
    if (testContext) {
      wrapWithApiLogging(this);
    }
  }
  static async loggedIn(testContext) {
    const api = new CustomerApi(testContext);
    const { email, name, password } = $.randomUserCredentials();
    const account = await api.auth.signup(email, name, password);
    await api.auth.login(email, password, $.customerOrg);
    api.account = { id: account.user.id, email, name, password };
    return api;
  }
}
