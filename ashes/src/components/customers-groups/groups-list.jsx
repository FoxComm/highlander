/* @flow */

// libs
import React, { Component } from 'react';
import { connect } from 'react-redux';

import { actions } from 'modules/customer-groups/list';

// components
import { SelectableSearchList } from 'components/list-page';
import MultiSelectRow from '../table/multi-select-row';

type Props = {
  list: Object;
}

const tableColumns = [
  { field: 'name', text: 'Group Name' },
  { field: 'customersCount', text: 'Customers Count' },
  { field: 'createdAt', type: 'date', text: 'Date/Time Created' },
  { field: 'modifiedAt', type: 'date', text: 'Date/Time Last Modified' },
];

function renderRow(row, index, columns, params) {
  return (
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

const GroupsList = ({ list }: Props) => (
  <SelectableSearchList
    entity="customerGroups.list"
    emptyMessage="No groups found."
    list={list}
    renderRow={renderRow}
    tableColumns={tableColumns}
    searchActions={actions}
    searchOptions={{ singleSearch: true }}
  />
);

const mapStateToProps = state => ({
  list: state.customerGroups.list,
});

export default connect(mapStateToProps, actions)(GroupsList);
