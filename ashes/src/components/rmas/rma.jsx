import React from 'react';
import PropTypes from 'prop-types';
import { connect } from 'react-redux';

import { IndexLink, Link } from 'components/link';
import Notes from '../notes/notes';
import { PageTitle } from '../section-title';
import { PrimaryButton } from 'components/core/button';
import PageNav from 'components/core/page-nav';
import { PanelList, PanelListItem } from '../panel/panel-list';
import ContentBox from '../content-box/content-box';
import State from '../common/state';

import * as rmaActions from '../../modules/rmas/details';

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

  get rma() {
    return this.props.rma.currentRma;
  }

  componentDidMount() {
    let { rma } = this.props.params;

    this.props.fetchRma(rma);
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
      const params = { rma: rma && rma.referenceNumber || '' };
      const content = React.cloneElement(this.props.children, { ...this.props, entity: rma });

      return (
        <div>
          <PageNav gutter={true}>
            <IndexLink to="rma-details" params={params}>Details</IndexLink>
            <Link to="rma-notifications" params={params}>Transaction Notifications</Link>
            <Link to="rma-activity-trail" params={params}>Activity Trail</Link>
          </PageNav>
          {content}
        </div>
      );
    }
  }

  get itemsCount() {
    return this.rma.lineItems.skus.length;
  }

  get orderSubtitle() {
    return `for order ${this.rma.orderRefNum}`;
  }

  render() {
    const rma = this.rma;

    if (!rma.id) {
      return null;
    }

    return (
      <div>
        <PageTitle title={`Return ${rma.referenceNumber}`} subtitle={this.orderSubtitle}>
          <PrimaryButton onClick={this.cancelReturn}>Cancel Return</PrimaryButton>
        </PageTitle>
        <div className="fc-grid fc-grid-match">
          <div className="fc-col-md-3-4">
            <PanelList>
              <PanelListItem title="Return State">
                <State value={this.rma.state} model="rma" />
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
