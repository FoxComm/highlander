/* @flow */

import React, { Component } from 'react';

// libs
import _ from 'lodash';
import { bindActionCreators } from 'redux';
import { connect } from 'react-redux';
import { autobind } from 'core-decorators';
import { bulkExportBulkAction, renderExportModal } from 'modules/bulk-export/helpers';

// components
import UserRow from './user-row';
import { SelectableSearchList } from 'components/list-page';
import BulkActions from 'components/bulk-actions/bulk-actions';
import BulkMessages from 'components/bulk-actions/bulk-messages';
import { Link } from 'components/link';

// actions
import { actions } from 'modules/users/list';
import { bulkExport } from 'modules/bulk-export/bulk-export';
import { actions as bulkActions } from 'modules/users/bulk';

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
  { field: 'image', text: 'Image' },
  { field: 'name', text: 'Name' },
  { field: 'email', text: 'Email' },
  { field: 'roles', text: 'Roles' },
  { field: 'createdAt', text: 'Created at', type: 'datetime' },
  { field: 'state', text: 'State' },
];

class Users extends Component {
  props: Props;

  renderRow(row: Object, index: number, columns: Columns, params: Object) {
    const key = `user-${row.id}`;

    return (
      <UserRow
        key={key}
        user={row}
        columns={columns}
        params={params}
      />
    );
  }
  @autobind
  bulkExport(allChecked: boolean, toggledIds: Array<number>) {
    const { exportByIds } = this.props.bulkActions;
    const modalTitle = 'Users';
    const entity = 'storeAdmins';

    return renderExportModal(tableColumns, entity, modalTitle, exportByIds, toggledIds);
  }

  get bulkActions(): Array<any> {
    return [
      bulkExportBulkAction(this.bulkExport, 'Users'),
    ];
  }

  renderBulkDetails(name: string, userId: string) {
    return (
      <span key={userId}>
        User <Link to="user" params={{ userId }}>{name}</Link>
      </span>
    );
  }

  render() {
    return (
      <div>
        <BulkMessages
          storePath="users.bulk"
          module="users"
          entity="user"
          renderDetail={this.renderBulkDetails}
        />
        <BulkActions
          module="users"
          entity="user"
          actions={this.bulkActions}
        >
          <SelectableSearchList
            exportEntity="storeAdmins"
            exportTitle="Users"
            bulkExport
            bulkExportAction={this.props.bulkExportAction}
            entity="users.list"
            emptyMessage="No users found."
            list={this.props.list}
            renderRow={this.renderRow}
            tableColumns={tableColumns}
            searchActions={this.props.actions}
          />
        </BulkActions>
      </div>
    );
  }
}

const mapStateToProps = (state) => {
  return {
    list: _.get(state.users, 'list', {}),
  };
};

const mapDispatchToProps = (dispatch) => {
  return {
    actions: bindActionCreators(actions, dispatch),
    bulkExportAction: bindActionCreators(bulkExport, dispatch),
    bulkActions: bindActionCreators(bulkActions, dispatch),
  };
};
export default connect(mapStateToProps, mapDispatchToProps)(Users);
