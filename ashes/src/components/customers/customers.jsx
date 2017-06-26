/* @flow */

import React, { Component } from 'react';

// libs
import _ from 'lodash';
import { autobind } from 'core-decorators';
import { bindActionCreators } from 'redux';
import { connect } from 'react-redux';
import { bulkExportBulkAction, renderExportModal } from 'modules/bulk-export/helpers';

// components
import { Link } from 'components/link';
import BulkActions from 'components/bulk-actions/bulk-actions';
import BulkMessages from 'components/bulk-actions/bulk-messages';
import SearchGroupModal from './groups/search-group-modal';
import { SelectableSearchList } from 'components/list-page';
import CustomerRow from './customer-row';

// actions
import { actions as bulkActions } from 'modules/customers/bulk';
import { actions } from 'modules/customers/list';
import { suggestGroups } from 'modules/customer-groups/suggest';
import { bulkExport } from 'modules/bulk-export/bulk-export';

type Props = {
  list: Object,
  actions: Object,
  tableColumns?: Array<Object>,
  suggestedGroups: Array<TCustomerGroupShort>,
  suggestState: Object,
  bulkActions: {
    addCustomersToGroup: (groupId: number, customersIds: Array<number>) => Promise<*>,
    reset: () => void,
    setMessages: (messages: Object) => void,
    exportByIds: (
      ids: Array<number>, description: string, fields: Array<Object>, entity: string, identifier: string
    ) => void,
  },
  bulkExportAction: (
    fields: Array<string>, entity: string, identifier: string, description: string
  ) => Promise<*>,
};

type State = {
  addToGroupModalShown: boolean,
  customerIds: Array<number>,
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

  get addToGroupAction(): Array<any> {
    return [
      'Add To Group',
      this.handleAddToGroup,
      'successfully added to group(s)',
      'could not be added to group(s)',
    ];
  }

  get bulkActions(): Array<any> {
    return [
      bulkExportBulkAction(this.bulkExport, 'Customers'),
      this.addToGroupAction,
    ];
  }

  renderRow(row: Object, index: number, columns: Columns, params: Object) {
    const key = `customer-${row.id}`;

    return (
      <CustomerRow
        key={key}
        customer={row}
        columns={columns}
        params={params}
      />
    );
  }

  @autobind
  handleAddToGroup(_, customerIds: Array<number>) {
    this.setState({
      addToGroupModalShown: true,
      customerIds,
    });
  }

  @autobind
  bulkExport(allChecked: boolean, toggledIds: Array<number>) {
    const { exportByIds } = this.props.bulkActions;
    const { tableColumns } = this.props;
    const modalTitle = 'Customers';
    const entity = 'customers';

    if (tableColumns) {
      return renderExportModal(tableColumns, entity, modalTitle, exportByIds, toggledIds);
    }
  }

  @autobind
  handleSelectGroup(groups: Array<TCustomerGroup>) {
    const customers = this.state.customerIds;
    const messages = this.addToGroupAction;
    const success = messages[2];
    const error = messages[3];

    this.props.bulkActions.reset();
    this.props.bulkActions.setMessages({ success, error });

    this.setState({
      addToGroupModalShown: false,
      customerIds: [],
    }, () =>
      groups.forEach(({ id }: TCustomerGroup) =>
        this.props.bulkActions.addCustomersToGroup(id, customers))
    );
  }

  renderBulkDetails(customerName, customerId) {
    return (
      <span key={customerId}>
        Customer <Link to="customer-details" params={{ customerId }}>{customerName}</Link>
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
            exportEntity="customers"
            exportTitle="Customers"
            bulkExport
            bulkExportAction={this.props.bulkExportAction}
            entity="customers.list"
            emptyMessage="No customers found."
            list={list}
            renderRow={this.renderRow}
            tableColumns={tableColumns}
            searchActions={actions}
            searchOptions={{singleSearch: true}}
          />
        </BulkActions>

        <SearchGroupModal
          isVisible={this.state.addToGroupModalShown}
          onCancel={() => this.setState({ addToGroupModalShown: false })}
          handleSave={this.handleSelectGroup}
          suggestGroups={actions.suggestGroups}
          suggested={suggestedGroups}
          suggestState={suggestState}
        />
      </div>
    );
  }
}

const mapState = state => ({
  list: state.customers.list,
  suggestedGroups: state.customerGroups.suggest.groups,
  suggestState: _.get(state.asyncActions, 'suggestGroups', {}),
});

const mapActions = (dispatch, props: Props) => ({
  actions: bindActionCreators({
    ...actions,
    suggestGroups: suggestGroups(),
  }, dispatch),
  bulkActions: bindActionCreators(bulkActions, dispatch),
  bulkExportAction: bindActionCreators(bulkExport, dispatch),
});

export default connect(mapState, mapActions)(Customers);
