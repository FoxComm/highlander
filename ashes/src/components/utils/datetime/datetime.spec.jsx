import React from 'react';
import { mount } from 'enzyme';

import * as DateTime from './datetime';

describe('DateTime', function () {

  it('should render emptyValue, when no value prop defined', function () {

    const datetime = mount(
      <DateTime.Moment />
    );

    expect(datetime.hasClass('time')).to.be.true;
    expect(datetime.text()).to.equal('not set');
  });

  it('should render date/time in (L LTS) format by default', function () {

    const datetime = mount(
      <DateTime.Moment value={'2017-05-19T13:51:50.417Z'} />
    );

    expect(datetime.find('time').hasClass('time')).to.be.true;
    expect(datetime.text()).to.equal('05/19/2017 4:51:50 PM');
  });

  it('should render date/time with defined format', function () {

    const datetime = mount(
      <DateTime.Moment value={'2017-05-19T13:51:50.417Z'} format="L LT" />
    );

    expect(datetime.text()).to.equal('05/19/2017 4:51 PM');
  });

  it('should render passed className inside Moment', function () {

    const datetime = mount(
      <DateTime.Moment className="new-className" />
    );

    expect(datetime.hasClass('new-className')).to.be.true;
  });

  it('should render date/time in (L LT) format', function () {

    const datetime = mount(
      <DateTime.DateTime value={'2017-05-19T13:51:50.417Z'} />
    );

    expect(datetime.text()).to.equal('05/19/2017 4:51 PM');
  });

  it('should render date in (L) format', function () {

    const datetime = mount(
      <DateTime.Date value={'2017-05-19T13:51:50.417Z'} />
    );

    expect(datetime.text()).to.equal('05/19/2017');
  });

  it('should render date/time in (LT) format', function () {

    const datetime = mount(
      <DateTime.Time value={'2017-05-19T13:51:50.417Z'} />
    );

    expect(datetime.text()).to.equal('4:51 PM');
  });

});
