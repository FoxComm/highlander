/**
 * @flow
 */

import { get, first, map } from 'lodash';
import { autobind } from 'core-decorators';
import React, { Component } from 'react';
import { bindActionCreators } from 'redux';
import { connect } from 'react-redux';

import { actions as bulkActions } from 'modules/customers/bulk';
import { actions } from 'modules/customers/list';
import { suggestGroups } from 'modules/customer-groups/suggest';

import { Link } from 'components/link';
import BulkActions from 'components/bulk-actions/bulk-actions';
import BulkMessages from 'components/bulk-actions/bulk-messages';
import SearchGroupModal from './groups/search-group-modal';
import { SelectableSearchList } from 'components/list-page';
import CustomerRow from './customer-row';

type Props = {
  list: Object,
  actions: Object,
  tableColumns?: Array<Object>,
  suggestedGroups: Array<TCustomerGroupShort>,
  suggestState: Object,
  bulkActions: {
    addCustomersToGroup: (groupId: number, customersIds: Array<number>) => Promise,
  },
};

type State = {
  addToGroupModalShown: boolean,
  customerIds: Array<number>,
};

function renderRow(row, index, columns, params) {
  return (
    <CustomerRow key={row.id} customer={row} columns={columns} params={params} />
  );
};

class Customers extends Component {
  props: Props;

  state: State = {
    addToGroupModalShown: false,
    customerIds: [],
  };

  static defaultProps = {
    tableColumns: [
      { field: 'name', text: 'Name' },
      { field: 'email', text: 'Email' },
      { field: 'id', text: 'Customer ID' },
      { field: 'shipRegion', text: 'Ship To Region' },
      { field: 'billRegion', text: 'Bill To Region' },
      { field: 'rank', text: 'Rank' },
      { field: 'joinedAt', text: 'Date/Time Joined', type: 'datetime' }
    ]
  };

  get bulkActions() {
    return [
      [
        'Add To Group',
        this.handleAddToGroup,
        'successfully added to group',
        'could not be added to group'
      ],
    ];
  }

  @autobind
  handleAddToGroup(_, customersIds) {
    const { suggestedGroups, suggestState, actions, bulkActions: { addCustomersToGroup } } = this.props;

    return (
      <SearchGroupModal
        isVisible={this.state.addToGroupModalShown}
        onCancel={() => this.setState({ addToGroupModalShown: false })}
        handleSave={(groups) => addCustomersToGroup(first(groups).id, customersIds)}
        suggestGroups={actions.suggestGroups}
        suggested={suggestedGroups}
        suggestState={suggestState}
      />
    );
  }

  @autobind
  handleSelectGroup(groups: Array<TCustomerGroup>) {
    const customers = this.state.customerIds;

    this.setState({
        addToGroupModalShown: false,
        customerIds: [],
      }, () =>
        groups.forEach(({ id }: TCustomerGroup) =>
          this.props.bulkActions.addCustomersToGroup(id, customers))
    );
  }

  renderBulkDetails(customerId) {
    return (
      <span key={customerId}>
        Customer <Link to="customer-details" params={{ customerId }}>{customerId}</Link>
      </span>
    );
  }

  render() {
    const { list, tableColumns, suggestedGroups, suggestState, actions } = this.props;

    return (
      <div>
        <BulkMessages
          storePath="customers.bulk"
          module="customers"
          entity="customer"
          renderDetail={this.renderBulkDetails}
        />
        <BulkActions
          module="customers"
          entity="customer"
          actions={this.bulkActions}
        >
          <SelectableSearchList
            entity="customers.list"
            emptyMessage="No customers found."
            list={list}
            renderRow={renderRow}
            tableColumns={tableColumns}
            searchActions={actions}
            searchOptions={{singleSearch: true}}
          />
        </BulkActions>
      </div>
    );
  }
}

const mapState = state => ({
  list: state.customers.list,
  suggestedGroups: state.customerGroups.suggest.groups,
  suggestState: get(state.asyncActions, 'suggestGroups', {}),
});

const mapActions = (dispatch, props: Props) => ({
  actions: bindActionCreators({
    ...actions,
    suggestGroups: suggestGroups(),
  }, dispatch),
  bulkActions: bindActionCreators(bulkActions, dispatch),
});

export default connect(mapState, mapActions)(Customers);
