import FoxCommApi from '@foxcomm/api-js';
import superagent from 'superagent';
import cookie from 'cookie';
import config from '../config';
import wrapWithApiLogging from './wrapWithApiLogging';

const endpoints = {
  esStoreAdmins: 'search/admin/store_admins_search_view/_search',
};

export default class Api extends FoxCommApi {
  constructor(options, testContext) {
    super(options);
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
  static withoutCookies(testContext) {
    return new Api({
      api_url: `${config.apiUrl}/api`,
      stripe_key: config.stripeKey,
    }, testContext);
  }
  static withCookies(testContext) {
    return new Api({
      api_url: `${config.apiUrl}/api`,
      stripe_key: config.stripeKey,
      agent: superagent.agent(),
    }, testContext);
  }
}
