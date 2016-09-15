/**
 * @flow
 */

import React, { Component, Element } from 'react';
import { autobind } from 'core-decorators';
import { assoc } from 'sprout-data';
import _ from 'lodash';

// components
import ConfirmationDialog from 'components/modal/confirmation-dialog';
import ContentBox from 'components/content-box/content-box';
import { FormField } from 'components/forms';
import VariantEntry from './option-entry';

// styles
import styles from './option-list.css';

// types
import type { Variant } from 'paragons/product';

type Props = {
  variants: { [key:string]: Variant },
};

type State = {
  newOption: { [key:string]: Variant },
  variants: { [key:string]: Variant },
};

class OptionList extends Component {
  props: Props;

  state: State = {
    newOption: null,
    variants: this.props.variants,
  };

  get actions(): Element {
    return (
      <a styleName="add-icon" onClick={this.handleAddNewOption}>
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

  @autobind
  handleAddNewOption() {
    this.setState({
      newOption: {
        name: '',
        type: '',
      },
    });
  }

  @autobind
  handleSaveNewOption(variant: Variant, listKey: string) {
    const { variants, newOption } = this.state;

    variants.push(newOption);

    this.setState({
      variants,
      newOption: null,
    });
  }

  @autobind
  handleCancelNewOption() {
    this.setState({
      newOption: null,
    });
  }

  renderOptions(variants: { [key:string]: Variant }): Array<Element> {
    return _.map(variants, (value, key) => {
      const reactKey = `product-variant-${key}`;
      return (
        <VariantEntry
          key={reactKey}
          variant={value}
          listKey={key}
          onCancel={this.handleCancelNewOption} />
      );
    });
  }

  @autobind
  handleDialogChange(value, field) {
    const newOption = assoc(this.state.newOption, field, value);

    this.setState({newOption});
  }

  renderNewOptionDialog() {
    const isVisible = !!this.state.newOption;
    const newOption = (
      <div styleName="new-option-dialog">
        <FormField
          className="fc-object-form__field"
          labelClassName="fc-object-form__field-label"
          label="Name"
          key={`object-form-attribute-name`}
        >
          <input
            type="text"
            value={this.state.newOption.name}
            onChange={({target}) => this.handleDialogChange(target.value, 'name')}
          />
        </FormField>
        <FormField
          className="fc-object-form__field"
          labelClassName="fc-object-form__field-label"
          label="Display Type"
          key={`object-form-attribute-type`}
        >
          <input
            type="text"
            value={this.state.newOption.type}
            onChange={({target}) => this.handleDialogChange(target.value, 'type')}
          />
        </FormField>
      </div>
    );

    return (
      <ConfirmationDialog
        isVisible={isVisible}
        header="New option"
        body={newOption}
        cancel="Cancel"
        confirm="Add option"
        cancelAction={this.handleCancelNewOption}
        confirmAction={this.handleSaveNewOption}
      />
    )
  }

  render(): Element {
    const variants = this.renderOptions(this.state.variants);
    const content = _.isEmpty(variants) ? this.emptyContent : variants;

    return (
      <ContentBox title="Options" actionBlock={this.actions}>
        {content}
        {this.state.newOption && this.renderNewOptionDialog()}
      </ContentBox>
    );
  }
}

export default OptionList;
