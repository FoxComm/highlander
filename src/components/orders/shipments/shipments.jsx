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
import ShipmentView from './shipment';
import UnshippedItemsView from './unshipped-items';

// types
import type AsyncState from 'lib/async-action-creator';
import type { Shipment, ShipmentLineItem, UnshippedLineItem } from 'paragons/shipment';


type Props = {
  shipments: Array<Shipment>;
  unshippedItems: Array<UnshippedLineItem>;
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

class Shipments extends Component<void, Props, void> {
  props: Props;

  componentDidMount(): void {
    this.props.actions.fetchShipments(this.props.entity.referenceNumber);
  }

  get controls() {
    return <PrimaryButton icon="add">Return</PrimaryButton>;
  }

  get data() {
    const { shipments, fetchShipments, unshippedItems} = this.props;

    if (fetchShipments.isRunning) {
      return <WaitAnimation />;
    }

    return (
      <div>
        {this.shipments}
        <UnshippedItemsView items={unshippedItems} />
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
          <ShipmentView
            key={index}
            index={index + 1}
            total={shipments.length}
            details={shipment} />
        ))}
      </div>
    );
  }

  render(): Element {
    return (
      <div>
        <SectionTitle title="Shipments" />
        {this.data}
      </div>
    );
  }
}

export default connect(mapStateToProps, mapDispatchToProps)(Shipments);
