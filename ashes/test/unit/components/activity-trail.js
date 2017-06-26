import { injectTimeMarks } from '../../../src/components/activity-trail/inject-time';

describe('Activity Trail component', function() {
  context('#injectTimeMarks', function() {
    before(() => {
      TimeShift.setTime(new Date('2015-12-08T12:43:10.319Z').getTime());
      TimeShift.setTimezoneOffset(0);
      global.Date = TimeShift.Date;
    });

    after(() => {
      global.Date = TimeShift.OriginalDate;
    });

    it('should insert `Today` for today _day_, and same for yesterday', function*() {
      const events = [
        { createdAt: '2015-12-08T06:43:10.319Z' },
        { createdAt: '2015-12-08T06:43:10.319Z' },
        { createdAt: '2015-12-07T22:43:10.319Z' },
        { createdAt: '2015-12-07T12:44:55.319Z' },
      ];

      const withTimeMarks = injectTimeMarks(events);

      expect(withTimeMarks[0].kind).to.equal('mark');
      expect(withTimeMarks[0].title).to.equal('Today');

      expect(withTimeMarks[3].kind).to.equal('mark');
      expect(withTimeMarks[3].title).to.equal('Yesterday');

      expect(withTimeMarks.length).to.equal(6);
    });

    it('should insert only `yesterday` for yesterday events', function*() {
      const events = [
        { createdAt: '2015-12-07T06:43:10.319Z' },
        { createdAt: '2015-12-07T06:43:10.319Z' },
        { createdAt: '2015-12-07T02:43:10.319Z' },
      ];

      const withTimeMarks = injectTimeMarks(events);

      expect(withTimeMarks[0].kind).to.equal('mark');
      expect(withTimeMarks[0].title).to.equal('Yesterday');
    });

    it('`today` and `yesterday` marks should depends of local timezone', function*() {
      TimeShift.setTimezoneOffset(-360);

      const events = [
        { createdAt: '2015-12-08T06:43:10.319Z' },
        { createdAt: '2015-12-08T06:43:10.319Z' },
        { createdAt: '2015-12-07T22:43:10.319Z' },
        { createdAt: '2015-12-07T12:44:55.319Z' },
      ];

      const withTimeMarks = injectTimeMarks(events);

      expect(withTimeMarks[0].kind).to.equal('mark');
      expect(withTimeMarks[0].title).to.equal('Today');

      expect(withTimeMarks[4].kind).to.equal('mark');
      expect(withTimeMarks[4].title).to.equal('Yesterday');

      expect(withTimeMarks.length).to.equal(6);

      TimeShift.setTimezoneOffset(0);
    });

    it('should insert year mark between two years', function*() {
      const events = [
        { createdAt: '2015-01-01T06:43:10.319Z' },
        { createdAt: '2015-01-01T06:43:10.319Z' },
        { createdAt: '2014-12-31T22:43:10.319Z' },
        { createdAt: '2014-12-31T12:44:55.319Z' },
      ];

      const withTimeMarks = injectTimeMarks(events);

      expect(withTimeMarks[0].kind).to.equal('mark');
      expect(withTimeMarks[0].title).to.equal('Jan 01');

      expect(withTimeMarks[3].kind).to.equal('year_mark');
      expect(withTimeMarks[3].title).to.equal('2014');

      expect(withTimeMarks[4].kind).to.equal('mark');
      expect(withTimeMarks[4].title).to.equal('Dec 31');
    });

    it('inserting years should depends of timezone also', function*() {
      TimeShift.setTimezoneOffset(-360);

      const events = [
        { createdAt: '2015-01-01T06:43:10.319Z' },
        { createdAt: '2015-01-01T06:43:10.319Z' },
        { createdAt: '2014-12-31T22:43:10.319Z' },
        { createdAt: '2014-12-31T12:44:55.319Z' },
        { createdAt: '2014-12-31T11:44:55.319Z' },
      ];

      const withTimeMarks = injectTimeMarks(events);

      expect(withTimeMarks[0].kind).to.equal('mark');
      expect(withTimeMarks[0].title).to.equal('Jan 01');

      expect(withTimeMarks[4].kind).to.equal('year_mark');
      expect(withTimeMarks[4].title).to.equal('2014');

      expect(withTimeMarks[5].kind).to.equal('mark');
      expect(withTimeMarks[5].title).to.equal('Dec 31');

      expect(withTimeMarks.length).to.equal(8);

      TimeShift.setTimezoneOffset(0);
    });

    it(`should insert year mark if we don't have activities for current year`, function*() {
      const events = [{ createdAt: '2014-12-31T12:44:55.319Z' }, { createdAt: '2014-12-31T12:42:55.319Z' }];

      const withTimeMarks = injectTimeMarks(events);

      expect(withTimeMarks[0].kind).to.equal('year_mark');
      expect(withTimeMarks[0].title).to.equal('2014');

      expect(withTimeMarks[1].kind).to.equal('mark');
      expect(withTimeMarks[1].title).to.equal('Dec 31');

      expect(withTimeMarks.length).to.equal(4);
    });
  });
});
