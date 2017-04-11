// libs
import _ from 'lodash';
import React, { Component } from 'react';
import { connect } from 'react-redux';
import { autobind } from 'core-decorators';

// components
import ActivityTrail from './activity-trail';
import ErrorAlerts from '../alerts/error-alerts';
import WaitAnimation from '../common/wait-animation';
import { SectionTitle } from '../section-title';

// redux
import { resetActivities, fetchActivityTrail } from 'modules/activity-trail';

type RequestParam = {
  dimension: string,
  objectId?: string | number,
}

type Props = {
  entity: {
    entityId: string | number,
    entityType: string,
  },
  trail: {
    activities: Array<Object>,
    hasMore: boolean,
  },
  route: {
    dimension: string,
  },
  fetchState: AsyncState,
  resetActivities: () => void;
  fetchActivityTrail: (params: RequestParam, from?: string) => Promise<*>,
};

class ActivityTrailPage extends Component {
  props: Props;

  get trailParams() {
    const { route, entity } = this.props;
    const dimension =
      route.dimension ? _.snakeCase(route.dimension) : _.snakeCase(entity.entityType);

    if (entity) {
      return {
        dimension,
        objectId: entity.entityId
      };
    } else {
      return {
        dimension
      };
    }
  }

  componentDidMount() {
    this.props.resetActivities();
    this.props.fetchActivityTrail(this.trailParams);
  }

  get content() {
    const { trail: { activities, hasMore }, fetchState } = this.props;

    const params = {
      activities,
      hasMore,
      fetchMore: this.fetchMore,
      fetchState,
    };

    if (fetchState.err) {
      return <ErrorAlerts error={err} />;
    }

    if (!activities.length && fetchState.inProgress) {
      return <WaitAnimation />;
    }

    return <ActivityTrail {...params} />;
  }

  @autobind
  fetchMore() {
    const { trail: { activities } } = this.props;

    if (!activities.length) return;

    const fromActivity = activities[activities.length - 1];

    this.props.fetchActivityTrail(this.trailParams, fromActivity);
  }

  render() {
    return (
      <div className="fc-activity-trail-page">
        <SectionTitle title="Activity Trail" />
        {this.content}
      </div>
    );
  }
}

const mapState = state => ({
  trail: state.activityTrail,
  fetchState: _.get(state, 'asyncActions.fetchActivityTrail', {}),
});

export default connect(mapState, { resetActivities, fetchActivityTrail })(ActivityTrailPage);
