/* @flow */

// libs
import _ from 'lodash';
import React, { Component } from 'react';
import { bindActionCreators } from 'redux';
import { connect } from 'react-redux';
import { autobind } from 'core-decorators';
import { bulkExportBulkAction, renderExportModal } from 'modules/bulk-export/helpers';

// components
import { SelectableSearchList } from 'components/list-page';
import MultiSelectRow from 'components/table/multi-select-row';
import { RoundedPill } from 'components/core/rounded-pill';
import BulkActions from 'components/bulk-actions/bulk-actions';
import BulkMessages from 'components/bulk-actions/bulk-messages';
import { Link } from 'components/link';

// actions
import { actions } from 'modules/customer-groups/list';
import { GROUP_TYPE_MANUAL, GROUP_TYPE_DYNAMIC, GROUP_TYPE_TEMPLATE } from 'modules/customer-groups/details/group';
import { bulkExport } from 'modules/bulk-export/bulk-export';
import { actions as bulkActions } from 'modules/customer-groups/bulk';

type Props = {
  list: Object,
  actions: Object,
  bulkExportAction: (
    fields: Array<string>, entity: string, identifier: string, description: string
  ) => Promise<*>,
  bulkActions: {
    exportByIds: (
      ids: Array<number>, description: string, fields: Array<Object>, entity: string, identifier: string
    ) => void,
  },
};

const tableColumns = [
  { field: 'name', text: 'Group Name' },
  { field: 'customersCount', text: 'Customers Count' },
  { field: 'groupType', text: 'Type' },
  { field: 'createdAt', type: 'datetime', text: 'Date/Time Created' },
  { field: 'updatedAt', type: 'datetime', text: 'Date/Time Last Modified' },
];

const groupTypes = {
  [GROUP_TYPE_MANUAL]: 'Manual',
  [GROUP_TYPE_DYNAMIC]: 'Dynamic',
  [GROUP_TYPE_TEMPLATE]: 'Template',
};

class GroupsList extends Component {
  props: Props;

  @autobind
  setCellContents(group: Object, field: string) {
    if (field == 'groupType') {
      const type = groupTypes[_.get(group, 'groupType', GROUP_TYPE_DYNAMIC)];
      return <RoundedPill text={type} />;
    }

    return _.get(group, field);
  }

  @autobind
  renderRow(row: Object, index: number, columns: Columns, params: Object) {
    return (
      <MultiSelectRow
        columns={columns}
        linkTo="customer-group"
        linkParams={{ groupId: row.id }}
        row={row}
        params={params}
        key={row.id}
        setCellContents={this.setCellContents}
      />
    );
  }

  @autobind
  bulkExport(allChecked: boolean, toggledIds: Array<number>) {
    const { exportByIds } = this.props.bulkActions;
    const modalTitle = 'Customer Groups';
    const entity = 'customerGroups';

    return renderExportModal(tableColumns, entity, modalTitle, exportByIds, toggledIds);
  }

  get bulkActions() {
    return [
      bulkExportBulkAction(this.bulkExport, 'Cutomer Groups'),
    ];
  }

  renderBulkDetails(groupName: string, groupId: number) {
    return (
      <span key={groupId}>
        Customer Group <Link to="customer-group" params={{ groupId }}>{groupName}</Link>
      </span>
    );
  }

  render() {
    return (
      <div>
        <BulkMessages
          storePath="customerGroups.bulk"
          module="customerGroups"
          entity="customer group"
          renderDetail={this.renderBulkDetails}
        />
        <BulkActions
          module="customerGroups"
          entity="customer group"
          actions={this.bulkActions}
        >
          <SelectableSearchList
            exportEntity="customerGroups"
            exportTitle="Customer Groups"
            bulkExport
            bulkExportAction={this.props.bulkExportAction}
            entity="customerGroups.list"
            emptyMessage="No groups found."
            list={this.props.list}
            renderRow={this.renderRow}
            tableColumns={tableColumns}
            searchActions={this.props.actions}
            searchOptions={{ singleSearch: true }}
          />
        </BulkActions>
      </div>
    );
  }
}

const mapState = state => ({
  list: _.get(state.customerGroups, 'list', {}),
});

const mapActions = dispatch => ({
  actions: bindActionCreators(actions, dispatch),
  bulkExportAction: bindActionCreators(bulkExport, dispatch),
  bulkActions: bindActionCreators(bulkActions, dispatch),
});

export default connect(mapState, mapActions)(GroupsList);
