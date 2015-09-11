'use strict';

import React from 'react';
import { RouteHandler } from 'react-router';
import { Link } from 'react-router';
import { listenTo, stopListeningTo, dispatch } from '../../lib/dispatcher';
import ReturnsStore from './store';
import Notes from '../notes/notes';
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

  render() {
    let retrn = this.state.return;
    let params = {return: retrn && retrn.referenceNumber || ''};

    return (
      <div id="return">
        <Viewers model='returns' modelId={retrn.id}/>

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
        <div className="gutter">
          <h2>notes</h2>
          <Notes/>
        </div>
        <div className="gutter">
          <ul className="tabbed-nav">
            <li><Link to="return-details" params={params}>Details</Link></li>
            <li><Link to="return-notifications" params={params}>Transaction Notifications</Link></li>
            <li><Link to="return-activity-trail" params={params}>Activity Trail</Link></li>
          </ul>
          <RouteHandler return={retrn} modelName="return"/>
        </div>
      </div>
    );
  }
}

Return.contextTypes = {
  router: React.PropTypes.func
};
