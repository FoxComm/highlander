/**
 * @flow
 */

// libs
import React, { Component, Element } from 'react';
import { autobind } from 'core-decorators';
import { bindActionCreators } from 'redux';
import { connect } from 'react-redux';
import { filterArchived } from 'elastic/archive';
import { bulkExportBulkAction } from 'modules/bulk-export/helpers';
import _ from 'lodash';

// components
import { SelectableSearchList } from '../list-page';
import PromotionRow from './promotion-row';
import BulkWrapper from '../discounts/bulk';
import { BulkExportModal } from '../bulk-actions/modal';

// actions
import { actions } from 'modules/promotions/list';
import { bulkExport } from 'modules/bulk-export/bulk-export';
import { actions as bulkActions } from 'modules/promotions/bulk';

type Props = {
  list: Object,
  actions: Object,
  bulkExportAction: (fields: Array<string>, entity: string, identifier: string) => Promise<*>,
  bulkActions: {
    exportByIds: (
      ids: Array<number>, description: string, fields: Array<string>, entity: string, identifier: string
    ) => void,
  },
};

const tableColumns = [
  {field: 'id', text: 'Promotion ID'},
  {field: 'promotionName', text: 'Name'},
  {field: 'applyType', text: 'Apply Type'},
  {field: 'totalUsed', text: 'Total Uses'},
  {field: 'currentCarts', text: 'Current Carts'},
  {field: 'createdAt', text: 'Date/Time Created', type: 'datetime'},
  {field: 'state', text: 'State'},
];

class Promotions extends Component {
  props: Props;

  @autobind
  addSearchFilters(filters: Array<SearchFilter>, initial: boolean = false) {
    return this.props.actions.addSearchFilters(filterArchived(filters), initial);
  }

  renderRow(row: Object, index: number, columns: Array<any>, params: any): Element<*> {
    const key = `promotion-${row.id}`;
    return (
      <PromotionRow
        promotion={row}
        columns={columns}
        key={key}
        params={params}
      />
    );
  }

  @autobind
  bulkExport(allChecked: boolean, toggledIds: Array<number>) {
    const { exportByIds } = this.props.bulkActions;
    const fields = _.map(tableColumns, c => c.field);
    const identifier = _.map(tableColumns, item => item.text).toString();

    return (
      <BulkExportModal
        count={toggledIds.length}
        onConfirm={(description) => exportByIds(toggledIds, description, fields, 'promotions', identifier)}
        title="Promotions"
      />
    );
  }

  get bulkActions() {
    return [
      bulkExportBulkAction(this.bulkExport, 'Promotions'),
    ];
  }

  render() {
    const {list, actions} = this.props;

    const searchActions = {
      ...actions,
      addSearchFilters: this.addSearchFilters,
    };

    return (
      <div className="promotions">
        <BulkWrapper
          onDelete={searchActions.refresh}
          entity="promotion"
          extraActions={this.bulkActions}
        >
          <SelectableSearchList
            exportEntity="promotions"
            bulkExport
            bulkExportAction={this.props.bulkExportAction}
            entity="promotions.list"
            emptyMessage="No promotions found."
            list={list}
            renderRow={this.renderRow}
            tableColumns={tableColumns}
            searchActions={searchActions}
          />
        </BulkWrapper>
      </div>
    );
  }
}

const mapStateToProps = (state: Object) => {
  return {
    list: state.promotions.list,
  };
};

const mapDispatchToProps = (dispatch: Function) => {
  return {
    actions: bindActionCreators(actions, dispatch),
    bulkExportAction: bindActionCreators(bulkExport, dispatch),
    bulkActions: bindActionCreators(bulkActions, dispatch),
  };
};

export default connect(mapStateToProps, mapDispatchToProps)(Promotions);
