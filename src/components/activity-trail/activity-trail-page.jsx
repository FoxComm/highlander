
// libs
import { get } from 'sprout-data';
import React, { PropTypes } from 'react';
import { connect } from 'react-redux';
import { autobind } from 'core-decorators';

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
    }).isRequired,
    fetchActivityTrail: PropTypes.func
  };

  componentDidMount() {
    this.props.fetchActivityTrail(this.props.entity);
  }

  get activities() {
    return get(this.props, [this.props.entity.entityId, 'activities'], []);
  }

  @autobind
  fetchMore() {
    const activities = this.activities;
    if (!activities.length) return;

    const fromActivity = activities[activities.length - 1];

    this.props.fetchActivityTrail(this.props.entity, fromActivity);
  }

  render() {
    const props = this.props;
    const activities = this.activities;
    const hasMore = get(props, [props.entity.entityId, 'hasMore'], false);

    const params = {
      activities,
      hasMore,
      fetchMore: this.fetchMore,
    };

    return (
      <div className="fc-activity-trail-page">
        <h2>Activity Trail</h2>
        <ActivityTrail {...params} />
      </div>
    );
  }
}
