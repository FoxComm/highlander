/* @flow */

// libs
import _ from 'lodash';
import React, { Component, Element } from 'react';
import { connect } from 'react-redux';
import { bindActionCreators } from 'redux';

// helpers
import { getStore } from '../../lib/store-creator';

// components


type Props = {
  load: Function;
  entity: {
    referenceNumber: string;
  };
};

const mapStateToProps = state => state.orders.shipments;
const mapDispatchToProps = dispatch => {
  const { actions } = getStore('orders.shipments');

  return bindActionCreators(actions, dispatch);
};

class Shipments extends Component<void, Props, void> {
  props: Props;

  componentDidMount(): void {
    this.props.load(this.props.entity.referenceNumber);
  }

  render(): Element {
    return (
      <div>
        Shipments here
      </div>
    );
  }
};

Shipments.propTypes = {
};

Shipments.defaultProps = {
};

export default connect(mapStateToProps, mapDispatchToProps)(Shipments);
