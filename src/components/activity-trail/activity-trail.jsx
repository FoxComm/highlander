
// libs
import _ from 'lodash';
import moment from 'moment';
import React, { PropTypes } from 'react';

// components
import Activity, { getActivityRepresentative } from './activities';

function createTimeMark(time, daysDiff) {
  switch (daysDiff) {
    case 0:
      return {
        type: 'mark',
        title: 'Today',
      };
    case 1:
      return {
        type: 'mark',
        title: 'Yesterday',
      };
    default:
      return {
        type: 'mark',
        title: time.format('MMM DD'),
      };
  }
}

export function injectTimeMarks(activities) {
  const now = moment(Date.now()).endOf('day');

  let latestMarkDiff = null;
  let latestYear = now.year();

  const flatMap = _.flow(_.map, _.flatten);

  return flatMap(activities, activity => {
    const activityTime = moment(activity.createdAt);
    const daysDiff = now.diff(activityTime, 'days');

    let result = [activity];

    if (daysDiff != latestMarkDiff) {
      latestMarkDiff = daysDiff;

      result = [createTimeMark(activityTime, daysDiff), ...result];
    }

    if (latestYear != activityTime.year()) {
      latestYear = activityTime.year();

      result = [{
        type: 'year_mark',
        title: `${activityTime.year()}`,
      }, ...result];
    }

    return result;
  });
}

const renderActivityItem = (activity, idx) => {
  switch (activity.type) {
    case 'mark':
      return (
        <li className="fc-activity-trail__mark" key={`mark_${idx}`}>
          {activity.title}
        </li>
      );
    case 'year_mark':
      return (
        <li className="fc-activity-trail__year-mark" key={`mark_${idx}`}>
          {activity.title}
        </li>
      );
    default:
      return <Activity activity={activity} key={`activity_${idx}`} />;
  }
};

const ActivityTrail = props => {
  // filter only known activities
  const activities = _.filter(props.activities, activity => !!getActivityRepresentative(activity));

  const withTimeMarks = injectTimeMarks(activities);

  return (
    <ul className="fc-activity-trail">
      {withTimeMarks.map((activity, idx) => renderActivityItem(activity, idx))}
    </ul>
  );
};

ActivityTrail.propTypes = {
  activities: PropTypes.array.isRequired,
};

export default ActivityTrail;
