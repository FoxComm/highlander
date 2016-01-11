
// libs
import { get } from 'sprout-data';
import React, { PropTypes } from 'react';
import { connect } from 'react-redux';
import { autobind } from 'core-decorators';

// components
import ActivityTrail from './activity-trail';
import ErrorAlerts from '../alerts/error-alerts';

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

  get isFetching() {
    return get(this.props, [this.props.entity.entityId, 'isFetching'], null);
  }

  get err() {
    return get(this.props, [this.props.entity.entityId, 'err']);
  }

  get content() {
    const props = this.props;
    const activities = this.activities;
    const hasMore = get(props, [props.entity.entityId, 'hasMore'], false);

    const params = {
      activities,
      hasMore,
      fetchMore: this.fetchMore,
    };

    if (this.isFetching === false) {
      if (!this.err) {
        return <ActivityTrail {...params} />;
      } else {
        return <ErrorAlerts errors={[this.err]} />;
      }
    } else if (this.isFetching === true) {
      return <div className="fc-wait-block"></div>;
    }
  }

  @autobind
  fetchMore() {
    const activities = this.activities;
    if (!activities.length) return;

    const fromActivity = activities[activities.length - 1];

    this.props.fetchActivityTrail(this.props.entity, fromActivity);
  }

  render() {
    return (
      <div className="fc-activity-trail-page">
        <h2>Activity Trail</h2>
        {this.content}
      </div>
    );
  }
}
