
import React from 'react';
import moment from 'moment';

describe('Countdown', function() {
  const Countdown = requireComponent('countdown/countdown.jsx');

  let countdown;

  afterEach(function() {
    if (countdown) {
      countdown.unmount();
      countdown = null;
    }
  });

  it('should render', function () {
    countdown = shallowRender(
      <Countdown endDate={moment().add(5, 'm').utc().format()} />
    );
    countdown.instance.tick();
    expect(countdown.props.children).to.match(/\d{2}:\d{2}:\d{2}/);
  });

  it('should create ending mode', function () {
    countdown = shallowRender(
      <Countdown endDate={moment().add(1, 'm').utc().format()} />
    );
    countdown.instance.tick();

    expect(countdown.props.className).to.contain('fc-countdown');
    expect(countdown.props.className).to.contain('fc-countdown_ending');
    expect(countdown.props.children).to.match(/\d{2}:\d{2}:\d{2}/);
  });
});
