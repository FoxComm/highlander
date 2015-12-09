
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
  const now = moment().endOf('day');

  let latestMarkDiff = null;

  const flatMap = _.flow(_.map, _.flatten);

  return flatMap(activities, activity => {
    const activityTime = moment(activity.createdAt);
    const daysDiff = now.diff(activityTime, 'days');

    if (daysDiff != latestMarkDiff) {
      latestMarkDiff = daysDiff;

      return [createTimeMark(activityTime, daysDiff), activity];
    }

    return activity;
  });
}

const renderActivityItem = (activity, idx) => {
  if (activity.type === 'mark') {
    return (
      <li className="fc-activity-trail__mark" key={`mark_${idx}`}>
        {activity.title}
      </li>
    );
  } else {
    return <Activity activity={activity} key={`activity_${idx}`} />;
  }
};

const ActivityTrailList = props => {
  // filter only known activities
  const activities = _.filter(props.activities, activity => !!getActivityRepresentative(activity));

  const withTimeMarks = injectTimeMarks(activities);

  return (
    <ul className="fc-activity-trail">
      {withTimeMarks.map((activity, idx) => renderActivityItem(activity, idx))}
    </ul>
  );
};

ActivityTrailList.propTypes = {
  activities: PropTypes.array.isRequired,
};

export default ActivityTrailList;
