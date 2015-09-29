'use strict';

import React from 'react';
import TableHead from '../tables/head';
import TableBody from '../tables/body';
import UserInitials from '../users/initials';
import ActivityTrailStore from './store';
import { pluralize } from 'fleck';

export default class ActivityTrail extends React.Component {

  constructor(props) {
    super(props);
    this.state = {
      activities: []
    };
  }

  componentDidMount() {
    let model = this.props.modelName;
    ActivityTrailStore.uriRoot = `${pluralize(model)}/${this.props[model].id}`;
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
          <TableBody columns={this.props.tableColumns} rows={this.state.activities} model={this.props.modelName}>
            <UserInitials/>
          </TableBody>
        </table>
      </div>
    );
  }
}

ActivityTrail.propTypes = {
  tableColumns: React.PropTypes.array,
  order: React.PropTypes.object,
  modelName: React.PropTypes.string
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
