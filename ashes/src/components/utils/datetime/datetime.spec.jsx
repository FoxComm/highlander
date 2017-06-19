import React from 'react';
import { mount } from 'enzyme';
import moment from 'moment';

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
    const date = '2017-12-29T15:10:30';
    const expectedResult = moment.utc(date).local().format('L LTS');
    const datetime = mount(
      <DateTime.Moment value={date} />
    );

    expect(datetime.find('time').hasClass('time')).to.be.true;
    expect(datetime.text()).to.equal(expectedResult);
  });

  it('should render date/time with defined format', function () {
    const date = '2017-12-29T15:10:30';
    const expectedResult = moment.utc(date).local().format('L LT');
    const datetime = mount(
      <DateTime.Moment value={date} format="L LT" />
    );

    expect(datetime.text()).to.equal(expectedResult);
  });

  it('should render passed className inside Moment', function () {

    const datetime = mount(
      <DateTime.Moment className="new-className" />
    );

    expect(datetime.hasClass('new-className')).to.be.true;
  });

  it('should render date/time in (L LT) format', function () {
    const date = '2017-12-29T15:10:30';
    const expectedResult = moment.utc(date).local().format('L LT');
    const datetime = mount(
      <DateTime.DateTime value={date} />
    );

    expect(datetime.text()).to.equal(expectedResult);
  });

  it('should render date in (L) format', function () {
    const date = '2017-12-29T15:10:30';
    const expectedResult = moment.utc(date).local().format('L');
    const datetime = mount(
      <DateTime.Date value={date} />
    );

    expect(datetime.text()).to.equal(expectedResult);
  });

  it('should render date/time in (LT) format', function () {
    const date = '2017-12-29T15:10:30';
    const expectedResult = moment.utc(date).local().format('LT');
    const datetime = mount(
      <DateTime.Time value={date} />
    );

    expect(datetime.text()).to.equal(expectedResult);
  });

});
