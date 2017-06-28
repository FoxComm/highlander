import { AdminApi } from '../helpers/Api';
import $ from '../payloads';
import isDate from '../helpers/isDate'
import { expect } from 'chai';
import * as step from '../helpers/steps';

describe('[bvt] Store Admins', function() {

  this.timeout(30000);

  it('[bvt] Can list store admins', async () => {
      const api = new AdminApi;
      await step.login(api, $.adminEmail, $.adminPassword, $.adminOrg);
      const storeAdmins = await step.getStoreAdmins(api);

      expect(storeAdmins).to.be.a('array');
      expect(storeAdmins.length >= 1);
      for (const storeAdmin of storeAdmins) {
          expect(storeAdmin.id).to.be.a('number');
          expect(storeAdmin.name).to.be.a('string');
          expect(storeAdmin.email).to.be.a('string');
          expect(storeAdmin.state).to.be.a('string');
          expect(storeAdmin.scope).to.be.a('string');
          expect(isDate(storeAdmin.createdAt)).to.be.true;
      }
  });

  it('[bvt] Can view store admin details', async () => {
      const api = new AdminApi;
      await step.login(api, $.adminEmail, $.adminPassword, $.adminOrg);
      const storeAdmins = await step.getStoreAdmins(api);
      const storeAdmin = await step.getStoreAdmin(api, storeAdmins);

      expect(storeAdmin.id).to.be.a('number');
      expect(storeAdmin.name).to.be.a('string');
      expect(storeAdmin.email).to.be.a('string');
      expect(storeAdmin.state).to.be.a('string');
  });

  it('[bvt] Can create a new store admin', async () => {
      const api = new AdminApi;
      await step.login(api, $.adminEmail, $.adminPassword, $.adminOrg);
      const payload = $.randomStoreAdminPayload();
      const adminUser = await step.createAdminUser(api, payload);

      expect(adminUser.id).to.be.a('number');
      expect(adminUser.state).to.equal('invited');
      expect(adminUser.name).to.equal(payload.name);
      expect(adminUser.email).to.equal(payload.email);
  });

  it('[bvt] Can update store admin details', async () => {
      const api = new AdminApi;
      await step.login(api, $.adminEmail, $.adminPassword, $.adminOrg);
      const adminUser = await step.createAdminUser(api, $.randomStoreAdminPayload());
      const updPayload = $.randomStoreAdminPayload();
      const updatedAdminUser = await step.updateAdminUser(api, adminUser.id, updPayload);

      expect(updatedAdminUser.id).to.equal(adminUser.id);
      expect(updatedAdminUser.state).to.equal(adminUser.state);
      expect(updatedAdminUser.name).to.equal(updPayload.name);
      expect(updatedAdminUser.email).to.equal(updPayload.email);
  });

});