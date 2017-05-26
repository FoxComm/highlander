
// libs
import _ from 'lodash';
import React from 'react';
import PropTypes from 'prop-types';
import { injectTimeMarks } from './inject-time';

// components
import Activity, { getActivityRepresentative } from './activities';
import { Button } from 'components/core/button';

const renderActivityItem = (activity, idx, list, hasMore) => {
  const isFirst = !hasMore && idx == list.length - 1;

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
        <Button onClick={props.fetchMore} isLoading={props.fetchState.inProgress}>Older...</Button>
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
