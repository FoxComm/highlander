/**
 * @flow
 */

// libs
import React, { Component, Element } from 'react';
import { autobind } from 'core-decorators';
import { bindActionCreators } from 'redux';
import { connect } from 'react-redux';

// components
import { SelectableSearchList } from '../list-page';
import PromotionRow from './promotion-row';
import BulkWrapper from '../discounts/bulk';

// redux
import { actions } from '../../modules/promotions/list';

// helpers
import { filterArchived } from 'elastic/archive';

type Props = {
  list: Object,
  actions: Object,
};

const mapStateToProps = (state: Object) => {
  return {
    list: state.promotions.list,
  };
};

const mapDispatchToProps = (dispatch: Function) => {
  return {
    actions: bindActionCreators(actions, dispatch),
  };
};

const tableColumns = [
  {field: 'id', text: 'Promotion ID'},
  {field: 'promotionName', text: 'Name'},
  {field: 'storefrontName', text: 'Storefront Name'},
  {field: 'applyType', text: 'Apply Type'},
  {field: 'totalUsed', text: 'Total Uses'},
  {field: 'currentCarts', text: 'Current Carts'},
  {field: 'createdAt', text: 'Date/Time Created', type: 'datetime'},
  {field: 'state', text: 'State'},
];

/* ::`*/
@connect(mapStateToProps, mapDispatchToProps)
/* ::`*/
export default class Promotions extends Component {
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
        <BulkWrapper entity="promotion">
          <SelectableSearchList
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
