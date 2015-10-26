'use strict';

const React = require('react');
const TestUtils = require('react-addons-test-utils');
const ReactDOM = require('react-dom');
const moment = require('moment');

describe('Countdown', function() {
  let Countdown = requireComponent('countdown/countdown.jsx');

  it('should render', function *() {
    let countdown = ReactDOM.render(
      <Countdown endDate={moment().add(5, 'm').utc().format()}/>
      , container);

    let countdownNode = TestUtils.findRenderedDOMComponentWithTag(countdown, 'div');

    expect(countdownNode).to.be.instanceof(Object);
    expect(countdownNode.className).to.contain('fc-countdown');
    expect(countdownNode.innerHTML).to.match(/\d{2}:\d{2}:\d{2}/);
  });

  it('should create ending mode', function *() {
    let countdown = ReactDOM.render(
      <Countdown endDate={moment().add(1, 'm').utc().format()}/>
      , container);
    let countdownNode = TestUtils.findRenderedDOMComponentWithTag(countdown, 'div');

    expect(countdownNode).to.be.instanceof(Object);
    expect(countdownNode.className).to.contain('fc-countdown');
    expect(countdownNode.className).to.contain('fc-countdown_ending');
    expect(countdownNode.innerHTML).to.match(/\d{2}:\d{2}:\d{2}/);
  });
});
