'use strict';

import React, { PropTypes } from 'react';
import { Link } from '../link';
import { listenTo, stopListeningTo, dispatch } from '../../lib/dispatcher';
import RmaStore from './store';
import Notes from '../notes/notes';
import Viewers from '../viewers/viewers';

export default class Rma extends React.Component {

  static propTypes = {
    params: PropTypes.shape({
      rma: PropTypes.string.isRequired
    }).isRequired,
    children: PropTypes.array
  };

  constructor(props, context) {
    super(props, context);
    this.state = {
      rma: {},
      pendingStatus: null
    };
  }

  componentDidMount() {
    let { rma }  = this.props.params;
    RmaStore.listenToEvent('change', this);
    RmaStore.fetch(rma);
  }

  componentWillUnmount() {
    RmaStore.stopListeningToEvent('change', this);
  }

  onChangeRmaStore(rma) {
    this.setState({
      rma: rma
    });
  }

  render() {
    let rma = this.state.rma;
    let params = {rma: rma && rma.referenceNumber || ''};
    let viewers = null;
    let notes = null;
    let subNav = null;
    let itemsCount = 0;

    const content = React.cloneElement(this.props.children, {rma, modelName: 'rma'});

    if (rma.id) {
      viewers = (
        <Viewers model='returns' modelId={rma.id}/>
      );
      notes = (
        <div className="gutter">
          <Notes return={rma} modelName={'return'}/>
        </div>
      );
      subNav = (
        <div className="gutter">
          <ul className="fc-tabbed-nav">
            <li><Link to="rma-details" params={params}>Details</Link></li>
            <li><Link to="rma-notifications" params={params}>Transaction Notifications</Link></li>
            <li><Link to="rma-activity-trail" params={params}>Activity Trail</Link></li>
          </ul>
          {content}
        </div>
      );
      itemsCount = rma.lineItems.length;
    }

    return (
      <div id="rma">
        {viewers}
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
            <dd>{itemsCount}</dd>
          </dl>
        </div>
        {notes}
        {subNav}
      </div>
    );
  }
}
