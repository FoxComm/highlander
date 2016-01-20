
// libs
import _ from 'lodash';
import moment from 'moment';
import React, { PropTypes } from 'react';

// components
import Activity, { getActivityRepresentative } from './activities';
import { Button } from '../common/buttons';

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
        kind: 'year_mark',
        title: `${activityTime.year()}`,
      }, ...result];
    }

    return result;
  });
}

const renderActivityItem = (activity, idx, list, hasMore) => {
  switch (activity.kind) {
    case 'mark':
      return (
        <li className="fc-activity-trail__mark" key={`mark_${idx}_${activity.title}`}>
          {activity.title}
        </li>
      );
    case 'year_mark':
      return (
        <li className="fc-activity-trail__year-mark" key={`mark_${idx}_${activity.title}`}>
          {activity.title}
        </li>
      );
    default:
      const isFirst = !hasMore && idx == list.length - 1;
      return <Activity activity={activity} isFirst={isFirst} key={`activity_${activity.id}_${activity.kind}`} />;
  }
};

const ActivityTrail = props => {
  // filter only known activities
  const activities = _.filter(props.activities, activity => !!getActivityRepresentative(activity));
  const withTimeMarks = injectTimeMarks(activities);

  let olderButton = null;

  if (props.hasMore) {
    olderButton = (
      <li className="fc-activity-trail__load-more">
        <Button onClick={props.fetchMore}>Older...</Button>
      </li>
    );
  }

  let noActivities = null;

  if (!withTimeMarks.length) {
    noActivities = (
      <li className="fc-activity-trail__no-activities">
        No activity yet.
      </li>
    );
  }

  return (
    <ul className="fc-activity-trail">
      {noActivities}
      {withTimeMarks.map((activity, idx) => renderActivityItem(activity, idx, withTimeMarks, props.hasMore))}
      {olderButton}
    </ul>
  );
};

ActivityTrail.propTypes = {
  activities: PropTypes.array.isRequired,
  hasMore: PropTypes.bool,
  fetchMore: PropTypes.func,
};

export default ActivityTrail;
