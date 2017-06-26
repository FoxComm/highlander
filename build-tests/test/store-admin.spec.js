import { AdminApi } from '../helpers/Api';
import isNumber from '../helpers/isNumber';
import isString from '../helpers/isString';
import isArray from '../helpers/isArray';
import isDate from '../helpers/isDate';
import $ from '../payloads';
import { expect } from 'chai';
import * as step from '../helpers/steps';

describe('[bvt] Store Admins', function() {

    this.timeout(8000);

    it('[bvt] Can list store admins', async () => {
        const api = new AdminApi;
        await step.login(api, $.adminEmail, $.adminPassword, $.adminOrg);
        const storeAdmins = await step.getStoreAdmins(api);

        expect(isArray(storeAdmins));
        expect(storeAdmins.length >= 1);
        for (const storeAdmin of storeAdmins) {
            expect(isNumber(storeAdmin.id)).to.be.true;
            expect(isString(storeAdmin.name)).to.be.true;
            expect(isString(storeAdmin.email)).to.be.true;
            expect(isString(storeAdmin.state)).to.be.true;
            expect(isString(storeAdmin.scope)).to.be.true;
            expect(isDate(storeAdmin.createdAt)).to.be.true;
        }
    });

    it('[bvt] Can view store admin details', async () => {
        const api = new AdminApi;
        await step.login(api, $.adminEmail, $.adminPassword, $.adminOrg);
        const storeAdmins = await step.getStoreAdmins(api);
        const storeAdmin = await step.getStoreAdmin(api, storeAdmins);

        expect(isNumber(storeAdmin.id)).to.be.true;
        expect(isString(storeAdmin.name)).to.be.true;
        expect(isString(storeAdmin.email)).to.be.true;
        expect(isString(storeAdmin.state)).to.be.true;
    });

    it('[bvt] Can create a new store admin', async () => {
        const api = new AdminApi;
        await step.login(api, $.adminEmail, $.adminPassword, $.adminOrg);
        const payload = $.randomStoreAdminPayload();
        const adminUser = await step.createAdminUser(api, payload);

        expect(isNumber(adminUser.id));
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