'use strict';

import React from 'react';
import { RouteHandler } from 'react-router';
import { Link } from 'react-router';
import { listenTo, stopListeningTo, dispatch } from '../../lib/dispatcher';
import ReturnsStore from './store';
import Viewers from '../viewers/viewers';

export default class Return extends React.Component {

  constructor(props) {
    super(props);
    this.state = {
      return: {},
      customer: {},
      pendingStatus: null
    };
  }

  componentDidMount() {
    let { router }  = this.context;
    let returnId = router.getCurrentParams().return;
    ReturnsStore.listenToEvent('change', this);
    ReturnsStore.fetch(returnId);
  }

  componentWillUnmount() {
    ReturnsStore.stopListeningToEvent('change', this);
  }

  onChangeReturnsStore(retrn) {
    this.setState({
      return: retrn,
      customer: retrn.customer
    });
  }

  onConfirmChange() {
    dispatch('toggleModal', null);
    this.patchOrder();
  }

  render() {
    let retrn = this.state.return;
    let subNav = null;
    let viewers = null;
    let returnStatus = null;

    //if (retrn.id) {
    //  let params = {return: retrn.referenceNumber};
    //
    //  subNav = (
    //    <div className="gutter">
    //      <ul className="tabbed-nav">
    //        <li><Link to="order-details" params={params}>Details</Link></li>
    //        <li><a href="">Shipments</a></li>
    //        <li><a href="">Returns</a></li>
    //        <li><Link to="order-notifications" params={params}>Transaction Notifications</Link></li>
    //        <li><Link to="order-notes" params={params}>Notes</Link></li>
    //        <li><Link to="order-activity-trail" params={params}>Activity Trail</Link></li>
    //      </ul>
    //      <RouteHandler order={order} modelName="order"/>
    //    </div>
    //  );
    //
    //  viewers = <Viewers model='returns' modelId={retrn.id}/>;
    //}
    //
    //if (ReturnsStore.editableStatusList.indexOf(order.orderStatus) !== -1) {
    //  returnStatus = (
    //    <select name="orderStatus" value={order.orderStatus} onChange={this.changeOrderStatus.bind(this)}>
    //      {ReturnsStore.selectableStatusList.map((status, idx) => {
    //        if (
    //          (order.orderStatus === 'fulfillmentStarted') &&
    //          (['fulfillmentStarted', 'canceled'].indexOf(status) === -1)
    //        ) {
    //          return '';
    //        } else {
    //          return <option key={`${idx}-${status}`} value={status}>{ReturnsStore.statuses[status]}</option>;
    //        }
    //      })}
    //    </select>
    //  );
    //} else {
    //  returnStatus = ReturnsStore.statuses[order.orderStatus];
    //}

    return (
      <div id="return">
        {viewers}
        <div className="gutter title">
          <div>
            <h1>Return {retrn.referenceNumber}</h1>
          </div>
        </div>
        <div className="gutter statuses">
          <dl>
            <dt>Return State</dt>
            <dd>{retrn.returnStatus}</dd>
          </dl>
          <dl>
            <dt>Return Type</dt>
            <dd>{retrn.returnType}</dd>
          </dl>
          <dl>
            <dt>Items</dt>
            <dd>{0}</dd>
          </dl>
        </div>
        {subNav}
      </div>
    );
  }
}

Return.contextTypes = {
  router: React.PropTypes.func
};
