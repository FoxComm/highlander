'use strict';

describe('Return Notifications', function() {

  context('#GET', function() {
    it('should get a list of notifications', function *() {
      let res = yield this.api.get('/returns/1/notifications');
      let notifications = res.response;

      expect(res.status).to.equal(200);
      expect(notifications).to.be.instanceof(Array);
    });
  });

  context('#POST', function() {
    it('should resend a notification', function *() {
      let data = {};
      let res = yield this.api.post('/returns/1/notifications/1', data);
      let notification = res.response;

      expect(res.status).to.equal(200);
      expect(notification.id).to.be.a('number');
      expect(notification.createdAt).to.match(/\d{4}-[01]\d-[0-3]\dT[0-2]\d:[0-5]\d:[0-5]\d\.\d+([+-][0-2]\d:[0-5]\d|Z)/);
      expect(notification.notificationStatus).to.be.an('string');
      expect(notification.subject).to.be.an('string');
      expect(notification.sendDate).to.match(/\d{4}-[01]\d-[0-3]\dT[0-2]\d:[0-5]\d:[0-5]\d\.\d+([+-][0-2]\d:[0-5]\d|Z)/);
      expect(notification.contact).to.be.an('string');
    });
  });

});
