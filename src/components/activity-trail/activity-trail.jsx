
// libs
import _ from 'lodash';
import moment from 'moment';
import React, { PropTypes } from 'react';

// components
import Activity from './activity';

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

const renderActivityItem = activity => {
  if (activity.type === 'mark') {
    return (
      <li className="fc-activity-trail__mark">
        {activity.title}
      </li>
    );
  } else {
    return <Activity activity={activity} />;
  }
};

const ActivityTrailList = props => {
  const withTimeMarks = injectTimeMarks(props.activities);

  return (
    <ul className="fc-activity-trail">
      {withTimeMarks.map(activity => renderActivityItem(activity))}
    </ul>
  );
};

ActivityTrailList.propTypes = {
  activities: PropTypes.array.isRequired,
};

export default ActivityTrailList;
