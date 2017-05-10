/**
 * @flow
 */

// libs
import React, { Component, Element } from 'react';
import { autobind } from 'core-decorators';
import { bindActionCreators } from 'redux';
import { connect } from 'react-redux';
import { filterArchived } from 'elastic/archive';

// components
import { SelectableSearchList } from '../list-page';
import PromotionRow from './promotion-row';
import BulkWrapper from '../discounts/bulk';

// actions
import { actions } from '../../modules/promotions/list';
import { bulkExport } from 'modules/bulk-export/bulk-export';

type Props = {
  list: Object,
  actions: Object,
  bulkExportAction: (fields: Array<string>, entity: string, identifier: string) => Promise<*>,
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
  };
};

export default connect(mapStateToProps, mapDispatchToProps)(Promotions);
