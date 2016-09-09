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
import { Button } from '../common/buttons';
import ButtonWithMenu from '../common/button-with-menu';
import LocalNav from '../local-nav/local-nav';
import WaitAnimation from '../common/wait-animation';
import ArchiveActionsSection from '../archive-actions/archive-actions';
import ErrorAlerts from '../alerts/error-alerts';

// actions
import * as SkuActions from 'modules/skus/details';
import * as ArchiveActions from 'modules/skus/archive';

//helpers
import { transitionTo } from 'browserHistory';
import { isArchived } from 'paragons/common';
import { SAVE_COMBO, SAVE_COMBO_ITEMS } from 'paragons/common';
import { isSkuValid } from 'paragons/sku';

// styles
import styles from '../discounts/page.css';

// types
import type { Sku } from 'modules/skus/details';

type Props = {
  actions: {
    newSku: () => void,
    fetchSku: (code: string, context?: string) => Promise,
    createSku: (sku: Sku, context?: string) => void,
    updateSku: (sku: Sku, context?: string) => void,
  },
  sku: ?Sku,
  err: ?Object,
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
      this.props.actions.fetchSku(this.entityId)
        .then(({payload}) => {
          if (isArchived(payload)) transitionTo('skus');
        });
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

  get preventSave(): boolean {
    const { sku } = this.state;
    if (sku) {
      return !isSkuValid(sku);
    }

    return true;
  }

  @autobind
  handleChange(sku: Sku): void {
    this.setState({ sku });
  }

  save() {
    let mayBeSaved = false;

    if (this.state.sku) {
      if (this.isNew) {
        mayBeSaved = this.props.actions.createSku(this.state.sku);
      } else {
        mayBeSaved = this.props.actions.updateSku(this.state.sku);
      }
    }

    return mayBeSaved;
  }

  @autobind
  handleSubmit() {
    this.save();
  }

  @autobind
  handleSelectSaving(value) {
    const mayBeSaved = this.save();
    if (!mayBeSaved) return;

    mayBeSaved.then(() => {
      switch (value) {
        case SAVE_COMBO.NEW:
          transitionTo('sku-details', { skuCode: 'new' });
          this.props.actions.newSku();
          break;
        case SAVE_COMBO.DUPLICATE:
          transitionTo('sku-details', { skuCode: 'new' });
          break;
        case SAVE_COMBO.CLOSE:
          transitionTo('skus');
          break;
      }
    });
  }

  renderArchiveActions() {
    return(
      <ArchiveActionsSection
        type="SKU"
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

  @autobind
  handleCancel(): void {
    transitionTo('skus');
  }

  get error(): ?Element {
    const { err } = this.props;
    if (!err) return null;

    const message = _.get(err, ['messages', 0], 'There was an error saving the sku.');
    return (
      <div styleName="error" className="fc-col-md-1-1">
        <ErrorAlerts error={message} />
      </div>
    );
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
          <Button
            type="button"
            onClick={this.handleCancel} >
            Cancel
          </Button>
          <ButtonWithMenu
            title="Save"
            menuPosition="right"
            onPrimaryClick={this.handleSubmit}
            onSelect={this.handleSelectSaving}
            isLoading={isUpdating}
            items={SAVE_COMBO_ITEMS}
            buttonDisabled={this.preventSave}
          />
        </PageTitle>
        <LocalNav>
          <IndexLink to="sku-details" params={params}>Details</IndexLink>
          <Link to="sku-images" params={params}>Images</Link>
          <Link to="sku-inventory-details" params={params}>Inventory</Link>
          <Link to="sku-notes" params={params}>Notes</Link>
          <Link to="sku-activity-trail" params={params}>Activity Trail</Link>
        </LocalNav>
        <div className="fc-grid">
          {this.error}
          <div className="fc-col-md-1-1 fc-col-no-overflow">
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
    err: _.get(state, ['skus', 'details', 'err']),
    isFetching: _.get(state, ['skus', 'details', 'isFetching']),
    isUpdating: _.get(state, ['skus', 'details', 'isUpdating']),
  }),
  dispatch => ({
    actions: bindActionCreators(SkuActions, dispatch),
    ...bindActionCreators(ArchiveActions, dispatch),
  })
)(SkuPage);
