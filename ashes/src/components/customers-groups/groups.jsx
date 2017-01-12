/* @flow */

// libs
import get from 'lodash/get';
import React, { Component } from 'react';
import { connect } from 'react-redux';
import { autobind } from 'core-decorators';
import { transitionTo } from 'browserHistory';

import * as actions from 'modules/customer-groups/list';

// components
import MultiSelectTable from '../table/multi-select-table';
import GroupRow from './group-row';

type Props = {
  list: Object;
  inProgress: boolean;
  fetch: () => Promise;
  updateStateAndFetch: () => Promise;
}

const tableColumns = [
  { field: 'name', text: 'Group Name' },
  { field: 'type', text: 'Type' },
  { field: 'customersCount', text: 'Customers Count' },
  { field: 'createdAt', type: 'date', text: 'Date/Time Created' },
  { field: 'modifiedAt', type: 'date', text: 'Date/Time Last Modified' },
];

class Groups extends Component {
  props: Props;

  componentDidMount() {
    this.props.fetch();
  }

  @autobind
  handleAddGroup() {
    transitionTo('new-dynamic-customer-group');
  }

  get renderRow() {
    return (row, index, columns, params) => {
      const key = `group-${row.id}`;

      return (
        <GroupRow
          key={key}
          group={row}
          columns={columns}
          params={params} />
      );
    };
  }

  render() {
    const { list, inProgress, updateStateAndFetch } = this.props;

    return (
      <div className="fc-customer-groups">
        <MultiSelectTable
          columns={tableColumns}
          data={list}
          renderRow={this.renderRow}
          setState={updateStateAndFetch}
          isLoading={inProgress}
          emptyMessage="No groups found." />
      </div>
    );
  }
}

const mapStateToProps = state => ({
  inProgress: get(state, 'customerGroups.list.isFetching', false),
  list: state.customerGroups.list,
});

export default connect(mapStateToProps, actions)(Groups);
