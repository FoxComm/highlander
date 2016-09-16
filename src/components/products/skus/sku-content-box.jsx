/**
 * @flow
 */

import React, { Component, Element, PropTypes } from 'react';
import { autobind } from 'core-decorators';
import _ from 'lodash';

import ContentBox from 'components/content-box/content-box';
import SkuList from './sku-list';
import ConfirmationDialog from 'components/modal/confirmation-dialog';

import styles from './sku-content-box.css';

import type { Product } from 'paragons/product';
import type { Sku } from 'modules/skus/details';

type UpdateFn = (code: string, field: string, value: any) => void;

type Props = {
  fullProduct: ?Product,
  updateField: UpdateFn,
  updateFields: (code: string, toUpdate: Array<Array<any>>) => void,
  variants: Array<any>,
};

type State = {
  addDialogIsShown: boolean,
};

class SkuContentBox extends Component {
  props: Props;
  state: State = {
    addDialogIsShown: false,
  };

  get actions(): Element {
    if (_.isEmpty(this.props.variants)) {
      return null;
    }

    return (
      <a styleName="add-icon" onClick={this.addAction}>
        <i className="icon-add" />
      </a>
    );
  }

  get addSkuDialog(): Element {
    const body = (
      <div styleName="add-dialog">
        <div styleName="dialog-subtitle">Available options:</div>
        <div styleName="dialog-items">
        </div>
      </div>
    );
    return (
      <ConfirmationDialog
        isVisible={this.state.addDialogIsShown}
        header="Add SKUs"
        body={body}
        cancel="Cancel"
        confirm="Add"
        cancelAction={() => this.closeAction()}
        confirmAction={() => this.closeAction()}
      />
    );
  }

  @autobind
  addAction() {
    this.setState({ addDialogIsShown: true });
  }

  @autobind
  closeAction() {
    this.setState({ addDialogIsShown: false });
  }

  render() {
    console.log('Sku Content Box', this.props.variants)
    return (
      <ContentBox title="SKUs" actionBlock={ this.actions }>
        <SkuList
          fullProduct={this.props.fullProduct}
          updateField={this.props.onSetSkuProperty}
          updateFields={this.props.onSetSkuProperties}
          variants={this.props.variants}
        />
        { this.addSkuDialog }
      </ContentBox>
    );
  }
}

export default SkuContentBox;
