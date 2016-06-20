/* @flow */

// libs
import _ from 'lodash';
import React, { Component, Element } from 'react';
import { connect } from 'react-redux';
import { bindActionCreators } from 'redux';

// helpers
import { getStore } from '../../lib/store-creator';

// components
import WaitAnimation from '../common/wait-animation';

// types
import type AsyncState from '../../lib/async-action-creator';


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

  render(): Element {
    const { shipments, unshippedItems, load } = this.props;
    if (load.isRunning) {
      return <WaitAnimation />;
    }

    return (
      <div>
        Shipments here
      </div>
    );
  }
};

export default connect(mapStateToProps, mapDispatchToProps)(Shipments);
