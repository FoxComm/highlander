/* @flow */

// libs
import React, { Component, Element } from 'react';
import { autobind } from 'core-decorators';
import { bindActionCreators } from 'redux';
import { connect } from 'react-redux';
import { filterArchived } from 'elastic/archive';
import { bulkExportBulkAction, renderExportModal } from 'modules/bulk-export/helpers';
import _ from 'lodash';

// components
import { SelectableSearchList } from '../list-page';
import PromotionRow from './promotion-row';
import BulkWrapper from '../discounts/bulk';

// actions
import { actions } from 'modules/promotions/list';
import { bulkExport } from 'modules/bulk-export/bulk-export';
import { actions as bulkActions } from 'modules/promotions/bulk';

type Props = {
  list: Object,
  actions: Object,
  bulkExportAction: (
    fields: Array<string>, entity: string, identifier: string, description: string, sort: Array<Object>
  ) => Promise<*>,
  bulkActions: {
    exportByIds: (
      ids: Array<number>, description: string, fields: Array<Object>, entity: string, identifier: string
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
    const modalTitle = 'Promotions';
    const entity = 'promotions';

    return renderExportModal(tableColumns, entity, modalTitle, exportByIds, toggledIds);
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
            exportTitle="Promotions"
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
    list: _.get(state.promotions, 'list', {}),
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
