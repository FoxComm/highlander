
import { injectTimeMarks } from '../../../src/components/activity-trail/activity-trail';

describe('Activity Trail', function() {
  context('#injectTimeMarks', function() {


    before(() => {
      TimeShift.setTime(new Date('2015-12-08T12:43:10.319Z').getTime());
      TimeShift.setTimezoneOffset(0);
      global.Date = TimeShift.Date;
    });

    after(() => {
      global.Date = TimeShift.OriginalDate;
    });

    it('should insert `Today` for today _day_, and same for yesterday', function* () {
      const events = [
        { createdAt: '2015-12-08T06:43:10.319Z' },
        { createdAt: '2015-12-08T06:43:10.319Z' },
        { createdAt: '2015-12-07T22:43:10.319Z' },
        { createdAt: '2015-12-07T12:44:55.319Z' },
      ];

      const withTimeMarks = injectTimeMarks(events);

      expect(withTimeMarks[0].type).to.equal('mark');
      expect(withTimeMarks[0].title).to.equal('Today');

      expect(withTimeMarks[3].type).to.equal('mark');
      expect(withTimeMarks[3].title).to.equal('Yesterday');


      expect(withTimeMarks.length).to.equal(6);
    });

    it('should insert only `yesterday` for yesterday events', function* () {
      const events = [
        { createdAt: '2015-12-07T06:43:10.319Z' },
        { createdAt: '2015-12-07T06:43:10.319Z' },
        { createdAt: '2015-12-07T02:43:10.319Z' },
      ];

      const withTimeMarks = injectTimeMarks(events);

      expect(withTimeMarks[0].type).to.equal('mark');
      expect(withTimeMarks[0].title).to.equal('Yesterday');
    });

    it('`today` and `yesterday` marks should depends of local timezone', function* () {
      TimeShift.setTimezoneOffset(-360);

      const events = [
        { createdAt: '2015-12-08T06:43:10.319Z' },
        { createdAt: '2015-12-08T06:43:10.319Z' },
        { createdAt: '2015-12-07T22:43:10.319Z' },
        { createdAt: '2015-12-07T12:44:55.319Z' },
      ];

      const withTimeMarks = injectTimeMarks(events);

      expect(withTimeMarks[0].type).to.equal('mark');
      expect(withTimeMarks[0].title).to.equal('Today');

      expect(withTimeMarks[4].type).to.equal('mark');
      expect(withTimeMarks[4].title).to.equal('Yesterday');

      expect(withTimeMarks.length).to.equal(6);

      TimeShift.setTimezoneOffset(0);
    });
  });
});
