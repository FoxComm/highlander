/**
 * @flow
 */

// libs
import _ from 'lodash';
import React, { Component, Element, PropTypes } from 'react';
import { autobind } from 'core-decorators';
import { bindActionCreators } from 'redux';
import { connect } from 'react-redux';

// components
import { Link, IndexLink } from '../link';
import { PageTitle } from '../section-title';
import { PrimaryButton } from '../common/buttons';
import LocalNav from '../local-nav/local-nav';
import WaitAnimation from '../common/wait-animation';
import ArchiveActionsSection from '../archive-actions/archive-actions';

// actions
import * as SkuActions from '../../modules/skus/details';
import * as ArchiveActions from '../../modules/skus/archive';

//helpers
import { transitionTo } from 'browserHistory';

// types
import type { Sku } from '../../modules/skus/details';

type Props = {
  actions: {
    newSku: () => void,
    fetchSku: (code: string, context?: string) => void,
    createSku: (sku: Sku, context?: string) => void,
    updateSku: (sku: Sku, context?: string) => void,
  },
  sku: ?Sku,
  isFetching: boolean,
  isUpdating: boolean,
  params: { skuCode: string },
  children: Element,
  archiveSku: Function,
};

type State = {
  sku: ?Sku,
};

class SkuPage extends Component {
  props: Props;

  state: State = {
    sku: this.props.sku,
  };

  componentDidMount(): void {
    if (this.isNew) {
      this.props.actions.newSku();
    } else {
      this.props.actions.fetchSku(this.entityId);
    }
  }

  componentWillReceiveProps({ sku, isFetching, isUpdating }: Props) {
    if (isFetching || isUpdating) {
      return;
    }

    this.setState({ sku });
  }

  get entityId(): string {
    return this.props.params.skuCode;
  }

  get isNew(): boolean {
    return this.entityId === 'new';
  }

  get code(): string {
    return _.get(this.props.sku, 'attributes.code.v', '');
  }

  get title(): string {
    const code = this.code;
    return this.isNew ? 'New SKU' : code.toUpperCase();
  }

  @autobind
  handleChange(sku: Sku): void {
    this.setState({ sku });
  }

  @autobind
  handleSubmit(): void {
    if (this.state.sku) {
      if (this.isNew) {
        this.props.actions.createSku(this.state.sku);
      } else {
        this.props.actions.updateSku(this.state.sku);
      }
    }
  }

  renderArchiveActions() {
    return(
      <ArchiveActionsSection type="SKU"
                             title={this.title}
                             archive={this.archiveSku} />
    );
  }

  @autobind
  archiveSku() {
    this.props.archiveSku(this.code).then(() => {
      transitionTo('skus');
    });
  }

  render(): Element {
    const { sku, isFetching, isUpdating, params } = this.props;

    if (!sku || isFetching) {
      return <div className="fc-sku"><WaitAnimation /></div>;
    }

    const children = React.cloneElement(this.props.children, {
      entity: { entityId: this.entityId, entityType: 'sku' },
      onChange: this.handleChange,
      sku: this.state.sku,
    });

    return (
      <div>
        <PageTitle title={this.title}>
          <PrimaryButton
            className="fc-product-details__save-button"
            type="submit"
            disabled={isUpdating}
            isLoading={isUpdating}
            onClick={this.handleSubmit}>
            Save
          </PrimaryButton>
        </PageTitle>
        <LocalNav>
          <IndexLink to="sku-details" params={params}>Details</IndexLink>
          <Link to="sku-images" params={params}>Images</Link>
          <Link to="sku-inventory-details" params={params}>Inventory</Link>
          <Link to="sku-notes" params={params}>Notes</Link>
          <Link to="sku-activity-trail" params={params}>Activity Trail</Link>
        </LocalNav>
        <div className="fc-grid">
          <div className="fc-col-md-1-1">
            {children}
          </div>
        </div>

        {!this.isNew && this.renderArchiveActions()}
      </div>
    );
  }
}

export default connect(
  state => ({
    sku: _.get(state, ['skus', 'details', 'sku']),
    isFetching: _.get(state, ['skus', 'details', 'isFetching']),
    isUpdating: _.get(state, ['skus', 'details', 'isUpdating']),
  }),
  dispatch => ({
    actions: bindActionCreators(SkuActions, dispatch),
    ...bindActionCreators(ArchiveActions, dispatch),
  })
)(SkuPage);
