
// libs
import { get } from 'sprout-data';
import React, { PropTypes } from 'react';
import { connect } from 'react-redux';

// components
import ActivityTrail from './activity-trail';

// redux
import * as ActivityTrailActions from '../../modules/activity-trail';

@connect(state => state.activityTrail, ActivityTrailActions)
export default class ActivityTrailPage extends React.Component {

  static propTypes = {
    entity: PropTypes.shape({
      entityId: PropTypes.string,
      entityType: PropTypes.string,
    }).isRequired
  };

  componentDidMount() {
    this.props.fetchActivityTrail(this.props.entity);
  }

  render() {
    const props = this.props;
    const activities = get(props, [props.entity.entityId, 'activities'], []);

    return (
      <div className="fc-activity-trail-page">
        <h2>Activity Trail</h2>
        <ActivityTrail activities={activities} />
      </div>
    );
  }
}
