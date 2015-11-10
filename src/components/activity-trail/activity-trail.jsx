'use strict';

import React, { PropTypes } from 'react';
import TableHead from '../tables/head';
import TableBody from '../tables/body';
import UserInitials from '../users/initials';
import ActivityTrailStore from '../../stores/activity-trail';
import { pluralize } from 'fleck';

export default class ActivityTrail extends React.Component {

  constructor(props, context) {
    super(props, context);
    this.state = {
      activities: []
    };
  }

  componentDidMount() {
    ActivityTrailStore.uriRoot = `${pluralize(this.props.entity.entityType)}/${this.props.entity.entityId}`;
    ActivityTrailStore.listenToEvent('change', this);
    ActivityTrailStore.fetch();
  }

  componentWillUnmount() {
    ActivityTrailStore.stopListeningToEvent('change', this);
  }

  onChangeActivityTrailStore(activities) {
    this.setState({activities: activities});
  }

  render() {
    return (
      <div id="activity-trail">
        <h2>Activity Trail</h2>
        <table className="fc-table">
          <TableHead columns={this.props.tableColumns}/>
          <TableBody columns={this.props.tableColumns} rows={this.state.activities}
                     model={this.props.entity.entityType}>
            <UserInitials/>
          </TableBody>
        </table>
      </div>
    );
  }
}

ActivityTrail.propTypes = {
  tableColumns: PropTypes.array,
  entity: PropTypes.object
};

ActivityTrail.defaultProps = {
  tableColumns: [
    {field: 'createdAt', text: 'Date/Time', type: 'date'},
    {field: 'user', text: 'Person', component: 'UserInitials'},
    {field: 'eventName', text: 'Event'},
    {field: 'previousState', text: 'Previous State'},
    {field: 'newState', text: 'New State'}
  ]
};
