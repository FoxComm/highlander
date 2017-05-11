/* @flow */

import React, { Element, Component } from 'react';

// libs
import _ from 'lodash';
import { bindActionCreators } from 'redux';
import { connect } from 'react-redux';
import { autobind } from 'core-decorators';
import { bulkExportBulkAction, getIdsByProps } from 'modules/bulk-export/helpers';

// components
import { SelectableSearchList } from '../list-page';
import CartRow from './cart-row';
import BulkActions from 'components/bulk-actions/bulk-actions';
import BulkMessages from 'components/bulk-actions/bulk-messages';
import { BulkExportModal } from 'components/bulk-actions/modal';

// actions
import { actions } from 'modules/carts/list';
import { bulkExport } from 'modules/bulk-export/bulk-export';
import { actions as bulkActions } from 'modules/carts/bulk';

const tableColumns = [
  {field: 'referenceNumber', text: 'Cart'},
  {field: 'createdAt', text: 'Date/Time Placed', type: 'datetime'},
  {field: 'customer.name', text: 'Customer Name'},
  {field: 'customer.email', text: 'Customer Email'},
  {field: 'grandTotal', text: 'Total', type: 'currency'}
];

type Props = {
  list: Object,
  actions: Object,
  bulkExportAction: (fields: Array<string>, entity: string, identifier: string) => Promise<*>,
  bulkActions: {
    exportByIds: (
      ids: Array<number>, description: string, fields: Array<string>, entity: string, identifier: string
    ) => void,
  },

}

class Carts extends Component {
  props: Props;

  get renderRow() {
    return (row, index, columns, params) => {
      const key = `cart-${row.referenceNumber}`;

      return (
        <CartRow
          cart={row}
          columns={columns}
          key={key}
          params={params}
        />
      );
    };
  }

  @autobind
  bulkExport(allChecked: boolean, toggledIds: Array<number>) {
    const { list } = this.props;
    const { exportByIds } = this.props.bulkActions;
    const fields = _.map(tableColumns, c => c.field);
    const identifier = _.map(tableColumns, item => item.text).toString();
    const results = list.currentSearch().results.rows;
    const ids = getIdsByProps('referenceNumber', toggledIds, results);

    return (
      <BulkExportModal
        count={toggledIds.length}
        onConfirm={(description) => exportByIds(ids, description, fields, 'carts', identifier)}
        title="Customers"
      />
    );
  }

  get bulkActions() {
    return [
      bulkExportBulkAction(this.bulkExport, 'Carts'),
    ];
  }

  renderBulkDetails(message, cart) {
    console.log('calling renderBulkDetails()');
    return (
      <span key={cart}>
        Cart <Link to="carts" params={{ cart }}>{cart}</Link>
      </span>
    );
  }

  render() {
    const { list, actions } = this.props;

    return (
      <div>
        <BulkMessages
          storePath="carts.bulk"
          module="carts"
          entity="cart"
          renderDetail={this.renderBulkDetails}
        />
        <BulkActions
          module="carts"
          entity="cart"
          actions={this.bulkActions}
        >
          <SelectableSearchList
            exportEntity="carts"
            bulkExport
            bulkExportAction={this.props.bulkExportAction}
            entity="carts.list"
            emptyMessage="No carts found."
            list={list}
            renderRow={this.renderRow}
            tableColumns={tableColumns}
            searchActions={actions}
            predicate={cart => cart.referenceNumber}
          />
        </BulkActions>
      </div>
    );
  }
}

const mapStateToProps = (state) => {
  return {
    list: _.get(state.carts, 'list', {}),
  };
};

const mapDispatchToProps = dispatch => {
  return {
    actions: bindActionCreators(actions, dispatch),
    bulkExportAction: bindActionCreators(bulkExport, dispatch),
    bulkActions: bindActionCreators(bulkActions, dispatch),
  };
};

export default connect(mapStateToProps, mapDispatchToProps)(Carts);
