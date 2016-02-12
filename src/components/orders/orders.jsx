// libs
import _ from 'lodash';
import React, { PropTypes } from 'react';
import { autobind } from 'core-decorators';
import { bindActionCreators } from 'redux';
import { connect } from 'react-redux';

// data
import { actions } from '../../modules/orders/list';
import * as bulkActions from '../../modules/orders/bulk';

// components
import { SearchableList } from '../list-page';
import OrderRow from './order-row';
import { CancelOrderModal } from './modal';
import { SuccessNotification, ErrorNotification } from '../bulk-actions/notifications';
import { Link } from '../link';


const mapStateToProps = ({orders: {list, bulk}}) => {
  return {
    list,
    bulk,
  };
};

const mapDispatchToProps = dispatch => {
  return {
    actions: bindActionCreators(actions, dispatch),
    bulkActions: bindActionCreators(bulkActions, dispatch),
  };
};

@connect(mapStateToProps, mapDispatchToProps)
export default class Orders extends React.Component {
  static propTypes = {
    list: PropTypes.object.isRequired,
    bulk: PropTypes.shape({
      successes: PropTypes.object,
      errors: PropTypes.object,
    }).isRequired,
    actions: PropTypes.objectOf(PropTypes.func).isRequired,
    bulkActions: PropTypes.objectOf(PropTypes.func).isRequired,
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
    modal: null,
  };

  @autobind
  hideModal() {
    this.setState({modal: null});
  }

  @autobind
  cancelOrders(allChecked, toggledIds) {
    const {reset, cancelOrders} = this.props.bulkActions;

    this.setState({
      modal: (
        <CancelOrderModal
          isVisible={true}
          count={toggledIds.length}
          onCancel={this.hideModal}
          onConfirm={(reasonId) => {
            reset();
            this.hideModal();
            cancelOrders(toggledIds, reasonId);
          }} />
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

  get bulkMessages() {
    const {successes, errors} = this.props.bulk;
    const {clearSuccesses, clearErrors} = this.props.bulkActions;

    const notifications = [];

    if (successes) {
      notifications.push(
        <SuccessNotification key="successes"
                             entityForms={['order', 'orders']}
                             overviewMessage="successfully canceled"
                             onHide={clearSuccesses}>
          {_.map(successes, (messages, referenceNumber) => (
            <span key={referenceNumber}>
              Order <Link to="order-details" params={{order: referenceNumber}}>{referenceNumber}</Link>
            </span>
          ))}
        </SuccessNotification>
      );
    }

    if (errors) {
      notifications.push(
        <ErrorNotification key="errors"
                           entityForms={['order', 'orders']}
                           overviewMessage="could not be canceled"
                           onHide={clearErrors}>
          {_.map(errors, (messages, referenceNumber) => (
            <span key={referenceNumber}>
              Order <Link to="order-details" params={{order: referenceNumber}}>{referenceNumber}</Link>
            </span>
          ))}
        </ErrorNotification>
      );
    }

    return notifications;
  }

  render() {
    const {list, actions} = this.props;

    return (
      <div>
        {this.bulkMessages}
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
