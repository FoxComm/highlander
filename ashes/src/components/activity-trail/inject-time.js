import moment from 'moment';
import _ from 'lodash';

function createTimeMark(time, daysDiff) {
  switch (daysDiff) {
    case 0:
      return {
        kind: 'mark',
        title: 'Today',
      };
    case 1:
      return {
        kind: 'mark',
        title: 'Yesterday',
      };
    default:
      return {
        kind: 'mark',
        title: time.format('MMM DD'),
      };
  }
}

export function injectTimeMarks(activities) {
  const now = moment(Date.now()).endOf('day');

  let latestMarkDiff = null;
  let latestYear = now.year();

  return _.flatMap(activities, activity => {
    const activityTime = moment.utc(activity.createdAt).local();
    const daysDiff = now.diff(activityTime, 'days');

    let result = [activity];

    if (daysDiff != latestMarkDiff) {
      latestMarkDiff = daysDiff;

      result = [createTimeMark(activityTime, daysDiff), ...result];
    }

    if (latestYear != activityTime.year()) {
      latestYear = activityTime.year();

      result = [{
        kind: 'year_mark',
        title: `${activityTime.year()}`,
      }, ...result];
    }

    return result;
  });
}
