/* @flow */

// libs
import _ from 'lodash';
import React, { Component, Element } from 'react';
import { connect } from 'react-redux';
import { bindActionCreators } from 'redux';

// helpers
import { getStore } from 'lib/store-creator';

// components
import WaitAnimation from 'components/common/wait-animation';
import { PrimaryButton } from 'components/common/buttons';
import SectionTitle from 'components/section-title/section-title';
import Shipment from './shipment';
import UnshippedItems from './unshipped-items';

// types
import type AsyncState from 'lib/async-action-creator';
import type { TShipment, TShipmentLineItem, TUnshippedLineItem } from 'paragons/shipment';


type Props = {
  shipments: Array<TShipment>;
  unshippedItems: Array<TUnshippedLineItem>;
  fetchShipments: AsyncState;
  actions: {
    fetchShipments: Function;
  };
  entity: {
    referenceNumber: string;
  };
};

const mapStateToProps = state => state.orders.shipments;
const mapDispatchToProps = dispatch => {
  const { actions } = getStore('orders.shipments');

  return {
    actions: bindActionCreators(actions, dispatch),
  };
};

class Shipments extends Component {
  props: Props;

  componentDidMount(): void {
    this.props.actions.fetchShipments(this.props.entity.referenceNumber);
  }

  get controls() {
    return <PrimaryButton icon="add">Return</PrimaryButton>;
  }

  get data() {
    const { fetchShipments, unshippedItems } = this.props;

    if (fetchShipments.isRunning) {
      return <WaitAnimation />;
    }

    return (
      <div>
        {this.shipments}
        <UnshippedItems items={unshippedItems} />
      </div>
    );
  }

  get shipments() {
    const { shipments } = this.props;
    if (!shipments.length) {
      return null;
    }

    return (
      <div>
        {shipments.map((shipment, index) => (
          <Shipment
            key={index}
            index={index + 1}
            total={shipments.length}
            details={shipment} />
        ))}
      </div>
    );
  }

  render() {
    return (
      <div>
        <SectionTitle title="Shipments" />
        {this.data}
      </div>
    );
  }
}

export default connect(mapStateToProps, mapDispatchToProps)(Shipments);
