// libs
import _ from 'lodash';
import React, { PropTypes } from 'react';
import { autobind } from 'core-decorators';
import { bindActionCreators } from 'redux';
import { connect } from 'react-redux';

//utils
import { ReasonType } from '../../lib/reason-utils';

// data
import { actions } from '../../modules/orders/list';
import * as bulkActions from '../../modules/orders/bulk';
import { fetchReasons } from '../../modules/reasons';

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

const mapStateToProps = ({orders: {list, bulk}, reasons}) => {
  return {
    list,
    bulk,
    cancellationReasons: _.get(reasons, ['reasons', ReasonType.CANCELLATION], []),
  };
};

const mapDispatchToProps = dispatch => {
  return {
    actions: bindActionCreators(actions, dispatch),
    bulkActions: bindActionCreators(bulkActions, dispatch),
    fetchReasons: () => dispatch(fetchReasons(ReasonType.CANCELLATION)),
  };
};

@connect(mapStateToProps, mapDispatchToProps)
export default class Orders extends React.Component {
  static propTypes = {
    list: PropTypes.object.isRequired,
    bulk: PropTypes.shape({
      errors: PropTypes.arrayOf(PropTypes.string),
    }).isRequired,
    cancellationReasons: PropTypes.object,
    actions: PropTypes.objectOf(PropTypes.func).isRequired,
    bulkActions: PropTypes.objectOf(PropTypes.func).isRequired,
    fetchReasons: PropTypes.func,
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

  componentDidMount() {
    this.props.fetchReasons();
  }

  @autobind
  hideModal() {
    this.setState({modal: null});
  }

  @autobind
  cancelOrders(allChecked, toggledIds) {
    const {bulkActions: {cancelOrders}, cancellationReasons} = this.props;

    this.setState({
      modal: (
        <CancelOrderModal
          isVisible={true}
          count={toggledIds.length}
          reasons={cancellationReasons}
          onCancel={this.hideModal}
          onConfirm={(reasonId) => {
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

  renderBulkErrors() {
    const {errors} = this.props.bulk;
    debugger;
  }

  render() {
    const {list, actions, bulk} = this.props;

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
        {bulk.errors ? this.renderBulkErrors() : null}
      </div>
    );
  }
}
