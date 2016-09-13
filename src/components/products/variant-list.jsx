/**
 * @flow
 */

import React, { Component, Element } from 'react';
import { autobind } from 'core-decorators';
import _ from 'lodash';

// components
import ConfirmationDialog from 'components/modal/confirmation-dialog';
import ContentBox from 'components/content-box/content-box';
import { FormField } from 'components/forms';
import VariantEntry from './variant-entry';

// styles
import styles from './variant-list.css';

// types
import type { Variant } from 'paragons/product';

type Props = {
  variants: { [key:string]: Variant },
};

type State = {
  newVariant: { [key:string]: Variant },
};

export default class VariantList extends Component<void, Props, State> {
  props: Props;

  state: State = {
    newVariant: null,
  };

  get actions(): Element {
    return (
      <a styleName="add-icon" onClick={this.handleAddNewVariant}>
        <i className="icon-add" />
      </a>
    );
  }

  get emptyContent(): Element {
    return (
      <div className="fc-content-box__empty-text">
        This product does not have variants.
      </div>
    );
  }

  get variantList(): Array<Element> {
    return [
      ...this.renderVariants(this.props.variants),
      // ...this.renderVariants(this.state.newVariants),
    ];
  }

  @autobind
  handleAddNewVariant() {
    // const newVariant: Variant = {
    //   name: null,
    //   type: null,
    //   values: {},
    // };
    //
    // const lastKey = _.findLastKey(this.state.newVariants);
    // const key = lastKey ? parseInt(lastKey) + 1 : 0;

    this.setState({
      newVariant: {
        name: null,
        type: null,
        values: {},
      },
    });
  }

  @autobind
  handleSaveNewVariant(variant: Variant, listKey: string) {
    console.log(variant);
  }

  @autobind
  handleCancelNewVariant() {
    this.setState({
      newVariant: null,
    });
  }

  renderVariants(variants: { [key:string]: Variant }): Array<Element> {
    return _.map(variants, (value, key) => {
      const reactKey = `product-variant-${key}`;
      return (
        <VariantEntry
          key={reactKey}
          variant={value}
          listKey={key}
          onSave={this.handleSaveNewVariant}
          onCancel={this.handleCancelNewVariant} />
      );
    });
  }

  renderNewVariantDialog() {
    const isVisible = !!this.state.newVariant;
    const newVariant = (
      <div styleName="new-option-dialog">
        <FormField
          className="fc-object-form__field"
          labelClassName="fc-object-form__field-label"
          label="Name"
          key={`object-form-attribute-name`}
          isRequired
        >
          <input
            type="text"
          />
        </FormField>
        <FormField
          className="fc-object-form__field"
          labelClassName="fc-object-form__field-label"
          label="Display Type"
          key={`object-form-attribute-type`}
          isRequired
        >
          <input
            type="text"
          />
        </FormField>
      </div>
    );

    return (
      <ConfirmationDialog
        isVisible={isVisible}
        header="New option"
        body={newVariant}
        cancel="Cancel"
        confirm="Add option"
        cancelAction={this.handleCancelNewVariant}
        confirmAction={this.props.addOption}
      />
    )
  }

  render(): Element {
    const variants = this.variantList;
    const content = _.isEmpty(variants) ? this.emptyContent : variants;
    return (
      <ContentBox title="Variants" actionBlock={this.actions}>
        {content}
        {this.renderNewVariantDialog()}
      </ContentBox>
    );
  }
}
