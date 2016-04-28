/**
 * @flow
 */

// libs
import React, { Component, Element } from 'react';
import { autobind } from 'core-decorators';
import { bindActionCreators } from 'redux';
import { connect } from 'react-redux';

// components
import BulkActions from '../bulk-actions/bulk-actions';
import BulkMessages from '../bulk-actions/bulk-messages';
import { SelectableSearchList } from '../list-page';
import PromotionRow from './promotion-row';
import BulkWrapper from '../discounts/bulk';

// redux
import { actions } from '../../modules/promotions/list';

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

  renderRow(row: Object, index: number, columns: Array<any>, params: any): Element {
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

  render(): Element {
    const {list, actions} = this.props;

    return (
      <div className="promotions">
        <BulkWrapper entity="promotion">
          <SelectableSearchList
            emptyMessage="No promotions found."
            list={list}
            renderRow={this.renderRow}
            tableColumns={tableColumns}
            searchActions={actions}
          />
        </BulkWrapper>
      </div>
    );
  }
}
