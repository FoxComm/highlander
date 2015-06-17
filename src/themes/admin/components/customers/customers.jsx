'use strict';

import React from 'react';
import TableHead from '../tables/head';
import TableBody from '../tables/body';

export default class Customers extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      tableRows: this.generateUsers()
    };
  }

  generateUsers() {
    let
      idx = 50,
      users = [],
      roles = ['Admin', 'User'],
      blocks = ['Yes', 'No'],
      causes = ['Zoolander', 'Twoo Wuv'];

    while (idx--) {
      users.push({
        firstName: `Westley ${idx}`,
        lastName: `Buttercup ${idx}`,
        email: `roberts${idx}@dread.com`,
        role: roles[~~(Math.random() * roles.length)],
        blocked: blocks[~~(Math.random() * blocks.length)],
        cause: causes[~~(Math.random() * causes.length)],
        dateJoined: new Date().toISOString()
      });
    }
    return users;
  }

  render() {
    return (
      <div id="users">
        <div className="gutter">
          <table className='listing'>
            <TableHead columns={this.props.tableColumns}/>
            <TableBody columns={this.props.tableColumns} rows={this.state.tableRows}/>
          </table>
        </div>
      </div>
    );
  }
}

Customers.propTypes = {
  tableColumns: React.PropTypes.array
};

Customers.defaultProps = {
  tableColumns: [
    {field: 'firstName', text: 'First Name'},
    {field: 'lastName', text: 'Last Name'},
    {field: 'email', text: 'Email'},
    {field: 'role', text: 'Role'},
    {field: 'blocked', text: 'Blocked'},
    {field: 'cause', text: 'Cause'},
    {field: 'dateJoined', text: 'Date Joined', type: 'date'}
  ]
};
