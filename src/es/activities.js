
import mockedActivities from './fixtures/activity-trail.json';
import moment from 'moment';

const applyQuery = (activities, query) => {
  return activities.reduce((acc, activity) => {
    if (activity.data.author.toLowerCase().indexOf(query.toLowerCase()) != -1) {
      return [...acc, activity];
    }
    return acc;
  }, []);
};

const applyFrom = (activities, date) => {
  return activities.filter(activity => new Date(activity.createdAt) < date);
};

const applyDays = (activities, days) => {
  if (!activities.length) return activities;

  const firstDate = moment(activities[0].createdAt).endOf('day');

  return activities.filter(activity => firstDate.diff(moment(activity.createdAt), 'days') <= days);
};


/**
 * Fetch activities from elastic search
 * @param {Number} from - id of Activity to fetch from
 * @param {Number} days - how many days for fetch
 * @param {String} query - filter query
 */
export function fetch(from = null, days = 2, query = null) {
  return new Promise(resolve => {
    let activities = [...mockedActivities];

    if (query) {
      activities = applyQuery(activities, query);
    }

    if (from !== null) {
      activities = applyFrom(activities, from);
    }

    const countBeforeLimit = activities.length;

    if (days) {
      activities = applyDays(activities, days);
    }

    const hasMore = activities.length < countBeforeLimit;

    resolve({
      activities,
      hasMore,
    });
  });
}
