'use strict';

import React, { PropTypes } from 'react';
import { Link } from '../link';
import Notes from '../notes/notes';
import Viewers from '../viewers/viewers';
import { connect } from 'react-redux';
import * as rmaActions from '../../modules/rmas/details';

@connect((state, props) => ({
  ...state.rmas.details[props.params.rma]
}), rmaActions)
export default class Rma extends React.Component {
  static propTypes = {
    params: PropTypes.shape({
      rma: PropTypes.string.isRequired
    }).isRequired,
    children: PropTypes.node
  };

  constructor(...args) {
    super(...args);
    this.state = {
      pendingStatus: null
    };
  }

  componentDidMount() {
    let { rma }  = this.props.params;

    this.props.fetchRmaIfNeeded(rma);
  }

  get viewers() {
    return <Viewers model='returns' modelId={this.props.rma.id} />;
  }

  get notes() {
    return (
      <div className="fc-grid">
        <div className="fc-col-md-1-1">
          <Notes return={this.props.rma} modelName={'return'} />
        </div>
      </div>
    );
  }

  get subNav() {
    const rma = this.props.rma;
    const params = {rma: rma && rma.referenceNumber || ''};
    const content = React.cloneElement(this.props.children, {rma, modelName: 'rma'});

    return (
      <div className="gutter">
        <ul className="fc-tabbed-nav">
          <li><Link to="rma-details" params={params}>Details</Link></li>
          <li><Link to="rma-notifications" params={params}>Transaction Notifications</Link></li>
          <li><Link to="rma-activity-trail" params={params}>Activity Trail</Link></li>
        </ul>
        {content}
      </div>
    );
  }

  get itemsCount() {
    return this.props.rma.lineItems.length;
  }

  render() {
    const rma = this.props.rma;
    const params = {rma: rma && rma.referenceNumber || ''};

    return (
      <div className="fc-rma">
        {this.viewers}
        <div className="gutter title">
          <div>
            <h1>Return {rma.referenceNumber}</h1>
          </div>
        </div>
        <div className="gutter statuses">
          <dl>
            <dt>Return State</dt>
            <dd>{rma.returnStatus}</dd>
          </dl>
          <dl>
            <dt>Return Type</dt>
            <dd>{rma.returnType}</dd>
          </dl>
          <dl>
            <dt>Items</dt>
            <dd>{this.itemsCount}</dd>
          </dl>
        </div>
        {this.notes}
        {this.subNav}
      </div>
    );
  }
}
