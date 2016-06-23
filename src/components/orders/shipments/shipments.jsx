/* @flow */

// libs
import _ from 'lodash';
import React, { Component, Element } from 'react';
import { connect } from 'react-redux';
import { bindActionCreators } from 'redux';

// helpers
import { getStore } from '../../../lib/store-creator';

// components
import WaitAnimation from '../../common/wait-animation';
import { PrimaryButton } from '../../../components/common/buttons';
import SectionTitle from '../../section-title/section-title';
import Shipment from './shipment';
import UnshippedItems from './unshipped-items';

// types
import type AsyncState from '../../../lib/async-action-creator';


type Props = {
  shipments: Array<Object>;
  unshippedItems: Array<Object>;
  load: AsyncState;
  actions: {
    load: Function;
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
    this.props.actions.load(this.props.entity.referenceNumber);
  }

  get controls() {
    return <PrimaryButton icon="add">Return</PrimaryButton>;
  }

  get data() {
    if (this.props.load.isRunning) {
      return <WaitAnimation />;
    }

    return (
      <div>
        {this.shipments}
        <UnshippedItems items={this.props.unshippedItems} />
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

  render(): Element {
    return (
      <div>
        <SectionTitle title="Shipments">
          {this.controls}
        </SectionTitle>
        {this.data}
      </div>
    );
  }
};

export default connect(mapStateToProps, mapDispatchToProps)(Shipments);
