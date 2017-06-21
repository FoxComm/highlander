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
            expect(isNumber(storeAdmin.id));
            expect(isString(storeAdmin.name));
            expect(isString(storeAdmin.email));
            expect(isString(storeAdmin.state));
            expect(isString(storeAdmin.scope));
            expect(isDate(storeAdmin.createdAt));
        }
    });

    it('[bvt] Can view store admin details', async () => {
        const api = new AdminApi;
        await step.login(api, $.adminEmail, $.adminPassword, $.adminOrg);
        const storeAdmins = await step.getStoreAdmins(api);
        const storeAdmin = await step.getStoreAdmin(api, storeAdmins);

        expect(isNumber(storeAdmin.id));
        expect(isString(storeAdmin.name));
        expect(isString(storeAdmin.email));
        expect(isString(storeAdmin.state));
    });

    it('[bvt] Can create a new store admin', async () => {
        const api = new AdminApi;
        await step.login(api, $.adminEmail, $.adminPassword, $.adminOrg);
        const payload = $.randomStoreAdminPayload();
        const adminUser = await step.createAdminUser(api, $.randomStoreAdminPayload());

        expect(isNumber(adminUser.id));
        expect(adminUser.state, 'invited');
        expect(adminUser.name, payload.name);
        expect(adminUser.email, payload.email);
    });

    it('[bvt] Can update store admin details', async () => {
        const api = new AdminApi;
        await step.login(api, $.adminEmail, $.adminPassword, $.adminOrg);
        const adminUser = await step.createAdminUser(api, $.randomStoreAdminPayload());
        const updPayload = $.randomStoreAdminPayload();
        const updatedAdminUser = await step.updateAdminUser(api, adminUser.id, $.randomStoreAdminPayload());

        expect(updatedAdminUser.id, adminUser.id);
        expect(updatedAdminUser.state, adminUser.state);
        expect(updatedAdminUser.name, updPayload.name);
        expect(updatedAdminUser.email, updPayload.email);
    });

});