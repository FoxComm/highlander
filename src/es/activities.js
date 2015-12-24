
import mockedActivities from './fixtures/activity-trail.json';
import moment from 'moment';
import searchActivities from '../elastic/activities';

const applyQuery = (activities, query) => {
  return activities.reduce((acc, activity) => {
    if (activity.data.author.toLowerCase().indexOf(query.toLowerCase()) != -1) {
      return [...acc, activity];
    }
    return acc;
  }, []);
};

const applyFrom = (activities, tailActivity) => {
  const date = new Date(tailActivity.createdAt);

  return activities.filter(activity => new Date(activity.createdAt) < date);
};

const applyDays = (activities, days) => {
  if (!activities.length) return activities;

  const firstDate = moment(activities[0].createdAt).endOf('day');

  return activities.filter(activity => firstDate.diff(moment(activity.createdAt), 'days') <= days);
};


/**
 * Fetch activities from elastic search
 * @param {Number} fromActivity - Activity to fetch from
 * @param {Number} days - how many days for fetch
 * @param {String} query - filter query
 */
export function fetch(fromActivity = null, days = 2, query = null) {
  searchActivities(fromActivity, days, query);

  return new Promise(resolve => {
    let activities = [...mockedActivities];

    if (query) {
      activities = applyQuery(activities, query);
    }

    if (fromActivity !== null) {
      activities = applyFrom(activities, fromActivity);
    }

    const countBeforeLimit = activities.length;

    if (days) {
      activities = applyDays(activities, days);
    }

    const hasMore = activities.length < countBeforeLimit;

    resolve({
      result: activities,
      hasMore,
    });
  });
}
