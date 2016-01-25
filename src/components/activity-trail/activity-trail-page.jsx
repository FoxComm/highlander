
// libs
import { get } from 'sprout-data';
import React, { PropTypes } from 'react';
import { connect } from 'react-redux';
import { autobind } from 'core-decorators';

// components
import ActivityTrail from './activity-trail';
import ErrorAlerts from '../alerts/error-alerts';
import WaitAnimation from '../common/wait-animation';

// redux
import * as ActivityTrailActions from '../../modules/activity-trail';

@connect(state => state.activityTrail, ActivityTrailActions)
export default class ActivityTrailPage extends React.Component {

  static propTypes = {
    entity: PropTypes.shape({
      entityId: PropTypes.oneOfType([
        PropTypes.string,
        PropTypes.number,
      ]),
      entityType: PropTypes.string,
    }),
    activities: PropTypes.array.isRequired,
    hasMore: PropTypes.bool,
    err: PropTypes.any,
    isFetching: PropTypes.bool,
    route: PropTypes.shape({
      dimension: PropTypes.string,
    }),
    fetchActivityTrail: PropTypes.func,
    resetActivities: PropTypes.func,
  };

  get trailParams() {
    const { route, entity } = this.props;

    if (route.dimension) {
      return {
        dimension: route.dimension
      };
    } else {
      return {
        dimension: entity.entityType,
        objectId: entity.entityId,
      };
    }
  }

  componentDidMount() {
    window.a = this.props.route;
    this.props.resetActivities();
    this.props.fetchActivityTrail(this.trailParams);
  }

  get content() {
    const { activities, hasMore, err, isFetching = null } = this.props;

    const params = {
      activities,
      hasMore,
      fetchMore: this.fetchMore,
    };

    if (isFetching === false) {
      if (!err) {
        return <ActivityTrail {...params} />;
      } else {
        return <ErrorAlerts errors={[err]} />;
      }
    } else if (isFetching === true) {
      return <WaitAnimation />;
    }
  }

  @autobind
  fetchMore() {
    const { activities } = this.props;
    if (!activities.length) return;

    const fromActivity = activities[activities.length - 1];

    this.props.fetchActivityTrail(this.trailParams, fromActivity);
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
