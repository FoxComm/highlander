import _ from 'lodash';
import React, { PropTypes } from 'react';
import { autobind } from 'core-decorators';

import ContentBox from 'components/content-box/content-box';
import TableView from 'components/table/tableview';
import GiftCard from './payments/gift-card';
import StoreCredit from './payments/store-credit';
import CreditCard from './payments/credit-card';
import PanelHeader from 'components/panel-header/panel-header';

const viewColumns = [
  {field: 'name', text: 'Method'},
  {field: 'amount', text: 'Amount', type: 'currency'},
  {field: 'status', text: 'Status'},
  {field: 'createdAt', text: 'Date/Time', type: 'datetime'},
];

export default class Payments extends React.Component {
  static propTypes = {
    details: PropTypes.shape({
      order: PropTypes.shape({
        paymentMethods: PropTypes.array
      })
    }).isRequired,
  };

  state = {
    showDetails: {},
  };

  get currentCustomerId() {
    return _.get(this.props, 'details.order.customer.id');
  }

  get paymentMethods() {
    return this.props.details.order.paymentMethods;
  }

  get viewContent() {
    if (_.isEmpty(this.paymentMethods)) {
      return <div className="fc-content-box__empty-text">No payment method applied.</div>;
    } else {
      return (
        <TableView
          columns={viewColumns}
          data={{rows: this.paymentMethods}}
          wrapToTbody={false}
          renderRow={this.renderRow()}
        />
      );
    }
  }

  @autobind
  renderRow() {
    return row => {
      const id = row.id || row.code;
      const Renderer = this.getRowRenderer(row.type);

      const props = {
        paymentMethod: row,
        editMode: false,
        customerId: this.currentCustomerId,
        ...this.props,
        order: _.get(this.props, 'details.order'),
        showDetails: this.state.showDetails[id],
        toggleDetails: () => {
          this.setState({
            showDetails: {
              ...this.state.showDetails,
              [id]: !this.state.showDetails[id],
            }
          });
        }
      };

      return <Renderer {...props} key={id} />;
    };
  }

  getRowRenderer(type) {
    switch(type) {
      case 'giftCard':
        return GiftCard;
      case 'creditCard':
        return CreditCard;
      case 'storeCredit':
        return StoreCredit;
    }
  }

  render() {
    const props = this.props;
    const title = <PanelHeader showStatus={false} text="Payment Method" />;

    return (
      <ContentBox
        className="fc-order-payment"
        title={title}
        indentContent={false}>
        {this.viewContent}
      </ContentBox>
    );
  }
}
