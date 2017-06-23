import React from 'react';
import moment from 'moment';
import { shallow } from 'enzyme';

import Countdown from './countdown';

describe('Countdown', function() {
  it('should render Countdown', function() {
    const countdown = shallow(<Countdown endDate={moment().add(5, 'm').utc().format()} />);

    countdown.instance().tick();

    expect(countdown.hasClass('countdown')).to.be.true;
    expect(countdown.text()).to.match(/\d{2}:\d{2}:\d{2}/);
  });

  it('should render ending mode', function() {
    const countdown = shallow(<Countdown endDate={moment().add(1, 'm').utc().format()} />);

    countdown.instance().tick();

    expect(countdown.hasClass('ending')).to.be.true;
    expect(countdown.text()).to.match(/\d{2}:\d{2}:\d{2}/);
  });

  it('should render frozen mode', function() {
    const countdown = shallow(<Countdown endDate={moment().add(1, 'm').utc().format()} frozen />);

    countdown.instance().tick();

    expect(countdown.hasClass('frozen')).to.be.true;
    expect(countdown.text()).to.match(/\d{2}:\d{2}:\d{2}/);
  });
});
