/* @flow */

// libs
import get from 'lodash/get';
import React, { Component } from 'react';
import { connect } from 'react-redux';

import * as actions from 'modules/customer-groups/list';

// components
import MultiSelectTable from '../table/multi-select-table';
import MultiSelectRow from '../table/multi-select-row';

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

class GroupsList extends Component {
  props: Props;

  componentDidMount() {
    this.props.fetch();
  }

  get renderRow() {
    return (row, index, columns, params) => (
      <MultiSelectRow
        columns={columns}
        linkTo="customer-group"
        linkParams={{ groupId: row.id }}
        row={row}
        params={params}
        key={row.id}
      />
    );
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

export default connect(mapStateToProps, actions)(GroupsList);
