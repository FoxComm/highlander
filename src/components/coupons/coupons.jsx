
// libs
import React, { Component } from 'react';
import { autobind } from 'core-decorators';
import { bindActionCreators } from 'redux';
import { connect } from 'react-redux';

// components
import BulkActions from '../bulk-actions/bulk-actions';
import BulkMessages from '../bulk-actions/bulk-messages';
import { SelectableSearchList } from '../list-page';
import CouponRow from  './coupon-row';

// redux
import { actions } from '../../modules/coupons/list';
import { actions as bulkActions } from '../../modules/coupons/bulk';


const mapStateToProps = (state) => {
  return {
    list: state.coupons.list,
  };
};

const mapDispatchToProps = dispatch => {
  return {
    actions: bindActionCreators(actions, dispatch),
    bulkActions: bindActionCreators(bulkActions, dispatch),
  };
};

const tableColumns = [
  {field: 'name', text: 'Name'},
  {field: 'storefrontName', text: 'Storefront Name'},
  {field: 'code', text: 'Code'},
  {field: 'totalUses', text: 'Total Uses'},
  {field: 'inCarts', text: 'Current Carts'},
  {field: 'createdAt', text: 'Date/Time Created', type: 'datetime'},
  {field: 'state', text: 'State', type: 'state'},
];

@connect(mapStateToProps, mapDispatchToProps)
export default class Promotions extends Component {

  renderRow() {
    return (row, index, columns, params) => {
      const key = `coupon-${coupon.id}`;

      return (
        <CouponRow
          coupon={row}
          columns={columns}
          key={key}
          params={params}
        />
      );
    };
  }

  bulkActions() {
    return [];
  }

  renderDetail() {
    return null;
  }

  render() {
    const {list, actions} = this.props;

    const entity = 'coupon';
    const module = `${entity}s`;

    return (
      <div className="coupons">
        <BulkMessages
          storePath={`${module}.bulk`}
          module={module}
          entity={entity}
          renderDetail={this.renderDetail} />
        <BulkActions
          module={module}
          entity={entity}
          actions={this.bulkActions()}>
          <SelectableSearchList
            emptyMessage="No coupons found."
            list={list}
            renderRow={this.renderRow}
            tableColumns={tableColumns}
            searchActions={actions}
          />
        </BulkActions>
      </div>
    );
  }
}
