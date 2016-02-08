// libs
import _ from 'lodash';
import React, { PropTypes } from 'react';
import { autobind } from 'core-decorators';
import { bindActionCreators } from 'redux';
import { connect } from 'react-redux';

// data
import { actions } from '../../modules/orders/list';

// components
import { SearchableList } from '../list-page';
import OrderRow from './order-row';
import { CancelOrderModal } from './modal';


const tableColumns = [
  {field: 'referenceNumber', text: 'Order', model: 'order'},
  {field: 'placedAt', text: 'Date/Time Placed', type: 'datetime'},
  {field: 'customer.name', text: 'Name'},
  {field: 'customer.email', text: 'Email'},
  {field: 'state', text: 'Order State', type: 'state', model: 'order'},
  {field: 'shipping.state', text: 'Shipment State', type: 'state', model: 'shipment'},
  {field: 'grandTotal', text: 'Total', type: 'currency'}
];

const mapStateToProps = state => ({list: state.orders.list});

const mapDispatchToProps = dispatch => {
  return {actions: bindActionCreators(actions, dispatch)};
};

@connect(mapStateToProps, mapDispatchToProps)
export default class Orders extends React.Component {
  static propTypes = {
    list: PropTypes.object.isRequired,
    actions: PropTypes.object.isRequired,
  };

  static tableColumns = [
    {field: 'referenceNumber', text: 'Order', model: 'order'},
    {field: 'placedAt', text: 'Date/Time Placed', type: 'datetime'},
    {field: 'customer.name', text: 'Name'},
    {field: 'customer.email', text: 'Email'},
    {field: 'state', text: 'Order State', type: 'state', model: 'order'},
    {field: 'shipping.state', text: 'Shipment State', type: 'state', model: 'shipment'},
    {field: 'grandTotal', text: 'Total', type: 'currency'}
  ];

  state = {
    modal: null
  };

  @autobind
  hideModal() {
    this.setState({modal: null});
  }

  @autobind
  cancelOrders(allChecked, toggledIds) {
    this.setState({
      modal: (
        <CancelOrderModal
          isVisible={true}
          onCancel={this.hideModal}
          onConfirm={()=>{}} />
      )
    });
  }

  get renderRow() {
    return (row, index, columns, params) => {
      const key = `order-${row.referenceNumber}`;
      return (
        <OrderRow
          order={row}
          columns={columns}
          key={key}
          params={params} />
      );
    };
  }

  render() {
    const {list, actions} = this.props;

    return (
      <div>
        <SearchableList
          emptyResultMessage="No orders found."
          list={list}
          renderRow={this.renderRow}
          tableColumns={Orders.tableColumns}
          searchActions={actions}
          bulkActions={[
            ['Cancel Orders', this.cancelOrders]
          ]}
          predicate={({referenceNumber}) => referenceNumber} />
        {this.state.modal}
      </div>
    );
  }
}
