import React from 'react';
import { mount } from 'enzyme';
import { DateTime, Date, Time } from './datetime';

describe('DateTime', () => {

  describe('#DateTime', () => {
    it('should render date/time in (L LT) format', () => {
      const datetime = mount(
        <DateTime value={'2017-12-29T15:10:30'} utc={false} />
      );

      expect(datetime.text()).to.equal('12/29/2017 3:10 PM');
    });

    it('should render default emptyValue when value is not defined', () => {
      const datetime = mount(
        <DateTime />
      );

      expect(datetime.text()).to.equal('not set');
    });

    it('should render custom emptyValue ', () => {
      const datetime = mount(
        <DateTime emptyValue="date is not set" />
      );

      expect(datetime.text()).to.equal('date is not set');
    });

    it('should render passed className', () => {
      const datetime = mount(
        <DateTime className="test" />
      );

      expect(datetime.hasClass('test')).to.be.true;
    });

  });

  describe('#Date', () => {
    it('should render date/time in (L) format', () => {
      const datetime = mount(
        <Date value={'2017-12-29T15:10:30'} utc={false} />
      );

      expect(datetime.text()).to.equal('12/29/2017');
    });

    it('should render default emptyValue when value is not defined', () => {
      const datetime = mount(
        <Date />
      );

      expect(datetime.text()).to.equal('not set');
    });

    it('should render custom emptyValue ', () => {
      const datetime = mount(
        <Date emptyValue="date is not set" />
      );

      expect(datetime.text()).to.equal('date is not set');
    });

    it('should render passed className', () => {
      const datetime = mount(
        <Date className="test" />
      );

      expect(datetime.hasClass('test')).to.be.true;
    });
  });

  describe('#Date', () => {
    it('should render date/time in (LT) format', () => {
      const datetime = mount(
        <Time value={'2017-12-29T15:10:30'} utc={false} />
      );

      expect(datetime.text()).to.equal('3:10 PM');
    });

    it('should render default emptyValue when value is not defined', () => {
      const datetime = mount(
        <Time />
      );

      expect(datetime.text()).to.equal('not set');
    });

    it('should render custom emptyValue ', () => {
      const datetime = mount(
        <Time emptyValue="time is not set" />
      );

      expect(datetime.text()).to.equal('time is not set');
    });

    it('should render passed className', () => {
      const datetime = mount(
        <Time className="test" />
      );

      expect(datetime.hasClass('test')).to.be.true;
    });
  });
});
