
// libs
import React, { Component } from 'react';
import { autobind } from 'core-decorators';
import { bindActionCreators } from 'redux';
import { connect } from 'react-redux';

// components
import BulkActions from '../bulk-actions/bulk-actions';
import BulkMessages from '../bulk-actions/bulk-messages';
import { SelectableSearchList } from '../list-page';

// redux
import { actions } from '../../modules/promotions/list';
import { actions as bulkActions } from '../../modules/promotions/bulk';

// styles
import styles from './promotions.css';


const mapStateToProps = (state) => {
  return {
    list: state.promotions.list,
  };
};

const mapDispatchToProps = dispatch => {
  return {
    actions: bindActionCreators(actions, dispatch),
    bulkActions: bindActionCreators(bulkActions, dispatch),
  };
};

const tableColumns = [
  {field: 'id', text: 'Promotion ID'},
  {field: 'name', text: 'Name'},
  {field: 'storefrontName', text: 'Storefront Name'},
  {field: 'applyType', text: 'Apply Type'},
  {field: 'totalSales', text: 'Total Uses'},
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
        <PromotionRow
          promotion={row}
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

    const entity = 'promotion';
    const module = `${entity}s`;

    return (
      <div styleName="promotions">
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
            emptyMessage="No promotions found."
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
