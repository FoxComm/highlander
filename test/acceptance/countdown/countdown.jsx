'use strict';

require('testdom')('<html><body></body></html>');

const React = require('react/addons');
const TestUtils = React.addons.TestUtils;
const path = require('path');
const moment = require('moment');

describe('Countdown', function() {
  let Countdown = require(path.resolve('src/themes/admin/components/countdown/countdown.jsx'));
  let container = null;

  beforeEach(function() {
    container = document.createElement('div');
  });

  afterEach(function(done) {
    React.unmountComponentAtNode(container);
    setTimeout(done);
  });

  it('should render', function *() {
    let countdown = React.render(
      <Countdown endDate={moment().add(5, 'm').utc().format()}/>
      , container);
    let countdownNode = TestUtils.findRenderedDOMComponentWithTag(countdown, 'div').getDOMNode();

    expect(countdownNode).to.be.instanceof(Object);
    expect(countdownNode.className).to.contain('fc-countdown');
    expect(countdownNode.innerHTML).to.be.equal('00:04:59');
  });

  it('should create ending mode', function *() {
    let countdown = React.render(
      <Countdown endDate={moment().add(1, 'm').utc().format()}/>
      , container);
    let countdownNode = TestUtils.findRenderedDOMComponentWithTag(countdown, 'div').getDOMNode();

    expect(countdownNode).to.be.instanceof(Object);
    expect(countdownNode.className).to.contain('fc-countdown');
    expect(countdownNode.className).to.contain('fc-countdown_ending');
    expect(countdownNode.innerHTML).to.be.equal('00:00:59');
  });
});
