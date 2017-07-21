// @flow

import _ from 'lodash';
import React, { Component } from 'react';
import { autobind } from 'core-decorators';

import styles from './product-description.css';

import Accordion from 'ui/accordion/accordion';
import renderSpecifications from './specifications';
import Modal from 'ui/modal/modal';
import Popup from './popup';

import * as warrantyData from './warranty.json';
import * as returnPolicyData from './returnPolicy.json';

// types
import type { Sku } from 'types/sku';

type Props = {
  sku: Sku,
  className?: string,
}

type State = {
  shownPopup: ?number,
}

const popups = {
  warranty: 1,
  returnPolicy: 2,
};

class ProductDescription extends Component {
  props: Props;
  state: State = {
    shownPopup: null,
  };

  @autobind
  toggleProductWarranty(event: ?SyntheticEvent) {
    if (event) {
      event.stopPropagation();
      event.preventDefault();
    }
    const isShown = this.state.shownPopup === popups.warranty;
    this.setState({
      shownPopup: isShown ? null : popups.warranty,
    });
  }

  @autobind
  toggleReturnPolicy(event: ?SyntheticEvent) {
    if (event) {
      event.stopPropagation();
      event.preventDefault();
    }
    const isShown = this.state.shownPopup === popups.returnPolicy;
    this.setState({
      shownPopup: isShown ? null : popups.returnPolicy,
    });
  }

  render() {
    const { sku } = this.props;

    const description = _.get(sku, 'attributes.description.v', '');
    const skuCode = _.get(sku, 'attributes.code.v', '');

    return (
      <div className={this.props.className}>
        <Modal isVisible={this.state.shownPopup === popups.warranty} hide={() => this.toggleProductWarranty()}>
          <Popup title={warrantyData.title} onClose={() => this.toggleProductWarranty()}>
            <div dangerouslySetInnerHTML={{__html: warrantyData.content}} />
          </Popup>
        </Modal>

        <Modal isVisible={this.state.shownPopup === popups.returnPolicy} hide={() => this.toggleReturnPolicy()}>
          <Popup title={returnPolicyData.title} onClose={() => this.toggleReturnPolicy()}>
            <div dangerouslySetInnerHTML={{__html: returnPolicyData.content}} />
          </Popup>
        </Modal>

        <Accordion styleName="first-accordion" title="About this item" isInitiallyCollapsed={false}>
          <div styleName="desc-content">
            <div styleName="prod-style"><strong>STYLE: </strong>{skuCode}</div>
            <div styleName="prod-desc">{description}</div>
            <div styleName="info-block couple-of-links">
              <button
                styleName="info-link"
                title="Product Warranty"
                onClick={this.toggleProductWarranty}
              >
                Product Warranty
              </button>
              <button
                styleName="info-link"
                title="Free Return"
                onClick={this.toggleReturnPolicy}
              >
                Free Return
              </button>
            </div>
          </div>
        </Accordion>
        <Accordion title="Features and Specifications">
          {renderSpecifications(sku)}
        </Accordion>
      </div>
    );
  }

}

export default ProductDescription;
