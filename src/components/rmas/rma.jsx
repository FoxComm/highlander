'use strict';

import React, { PropTypes } from 'react';
import { IndexLink, Link } from '../link';
import Notes from '../notes/notes';
import Viewers from '../viewers/viewers';
import { connect } from 'react-redux';
import * as rmaActions from '../../modules/rmas/details';
import SectionTitle from '../section-title/section-title';
import { PrimaryButton } from '../common/buttons';
import LocalNav from '../local-nav/local-nav';
import { PanelList, PanelListItem } from '../panel/panel-list';
import { rmaStatuses } from '../../lib/format';
import ContentBox from '../content-box/content-box';

@connect((state, props) => ({
  rma: state.rmas.details
}), rmaActions)
export default class Rma extends React.Component {
  static propTypes = {
    params: PropTypes.shape({
      rma: PropTypes.string.isRequired
    }).isRequired,
    rma: PropTypes.shape({
      currentRma: PropTypes.object
    }),
    children: PropTypes.node,
    fetchRma: PropTypes.func.isRequired
  };

  constructor(...args) {
    super(...args);
    this.state = {
      pendingStatus: null
    };
  }

  get rma() {
    return this.props.rma.currentRma;
  }

  componentDidMount() {
    let { rma }  = this.props.params;

    this.props.fetchRma(rma);
  }

  get viewers() {
    return <Viewers model='returns' modelId={this.rma.id} />;
  }

  get notes() {
    if (!this.rma.entityId) return null;

    return (
      <div className="fc-grid fc-grid-gutter">
        <div className="fc-col-md-1-1">
          <Notes entity={this.rma} />
        </div>
      </div>
    );
  }

  get subNav() {
    const rma = this.rma;
    if (rma.id) {
      const params = {rma: rma && rma.referenceNumber || ''};
      const content = React.cloneElement(this.props.children, {...this.props, entity: rma});

      return (
        <div>
          <LocalNav gutter={true}>
            <IndexLink to="rma-details" params={params}>Details</IndexLink>
            <Link to="rma-notifications" params={params}>Transaction Notifications</Link>
            <Link to="rma-activity-trail" params={params}>Activity Trail</Link>
          </LocalNav>
          {content}
        </div>
      );
    }
  }

  get itemsCount() {
    return 0;
    //return this.rma.lineItems.length;
  }

  get orderSubtitle() {
    return `for order ${this.rma.orderId}`;
  }

  get returnState() {
    return rmaStatuses[this.rma.status];
  }

  render() {
    const rma = this.rma;
    const params = {rma: rma && rma.referenceNumber || ''};

    return (
      <div>
        {this.viewers}
        <SectionTitle title={`Return ${rma.referenceNumber}`} subtitle={this.orderSubtitle}>
          <PrimaryButton onClick={this.cancelReturn}>Cancel Return</PrimaryButton>
        </SectionTitle>
        <div className="fc-grid fc-grid-match">
          <div className="fc-col-md-3-4">
            <PanelList>
              <PanelListItem title="Return State">
                {this.returnState}
              </PanelListItem>
              <PanelListItem title="Return Type">
                {rma.rmaType}
              </PanelListItem>
              <PanelListItem title="Items">
                0
              </PanelListItem>
            </PanelList>
          </div>
          <div className="fc-col-md-1-4">
            <ContentBox title="Assignees">
              <div>1</div>
            </ContentBox>
          </div>
        </div>
        {this.notes}
        {this.subNav}
      </div>
    );
  }
}
