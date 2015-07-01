'use strict';

describe('Order Notifications #GET', function() {

  it('should get a list of notifications', function *() {
    let
      res     = yield this.api.get('/orders/1/notifications'),
      notifications   = res.response;
    expect(res.status).to.equal(200);
    expect(notifications).to.have.length.of.at.most(9);
  });
});

describe('Order Notifications #POST', function() {

  it('should resend a notification', function *() {
    let
      data    = {},
      res     = yield this.api.post('/orders/1/notifications/1', data),
      notification    = res.response;

    expect(res.status).to.equal(200);
    expect(notification.id).to.be.a('number');
    expect(notification.createdAt).to.match(/\d{4}-[01]\d-[0-3]\dT[0-2]\d:[0-5]\d:[0-5]\d\.\d+([+-][0-2]\d:[0-5]\d|Z)/);
    expect(notification.notificationStatus).to.be.an('string');
    expect(notification.subject).to.be.an('string');
    expect(notification.sendDate).to.match(/\d{4}-[01]\d-[0-3]\dT[0-2]\d:[0-5]\d:[0-5]\d\.\d+([+-][0-2]\d:[0-5]\d|Z)/);
    expect(notification.contact).to.be.an('string');
  });
});
