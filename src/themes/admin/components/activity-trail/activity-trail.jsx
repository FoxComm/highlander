'use strict';

import React from 'react';
import TableHead from '../tables/head';
import TableBody from '../tables/body';
import UserInitials from '../users/initials';
import ActivityTrailStore from './store';
import { pluralize } from 'fleck';

export default class ActivityTrail extends React.Component {
  componentDidMount() {
    ActivityTrailStore.listenToEvent('change', this);
  }

  componentWillUnmount() {
    ActivityTrailStore.stopListeningToEvent('change', this);
  }

  render() {
    let model = this.props[this.props.modelName];

    return (
      <div id="activity-trail">
        Yo
      </div>
    );
  }
}

ActivityTrail.propTypes = {
  order: React.PropTypes.object,
  modelName: React.PropTypes.string
};
