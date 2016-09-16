/**
 * @flow
 */

import React, { Component, Element } from 'react';
import { autobind } from 'core-decorators';
import _ from 'lodash';

// components
import ContentBox from 'components/content-box/content-box';
import { FormField } from 'components/forms';
import OptionEntry from './option-entry';
import OptionEditDialog from './option-edit-dialog';

// styles
import styles from './option-list.css';

// types
import type { Variant } from 'paragons/product';

type Props = {
  variants: { [key:string]: Variant },
};

type State = {
  editOption: bool,
  variants: { [key:string]: Variant },
};

class OptionList extends Component {
  props: Props;

  state: State = {
    editOption: null,
    variants: this.props.variants,
  };

  get actions(): Element {
    return (
      <a styleName="action-icon" onClick={() => this.startEditOption('new')}>
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
  startEditOption(id) {
    let editOption = { id };

    if (id !== 'new') {
      editOption.option = this.state.variants[id]
    } else {
      editOption.option = {
        name: '',
        type: '',
      }
    }

    this.setState({
      editOption
    });
  }

  @autobind
  deleteOption(id) {
    const { variants } = this.state;

    variants.splice(id, 1);

    this.setState({
      variants
    });
  }

  @autobind
  updateOption(option, id) {
    const { variants } = this.state;

    if (id === 'new') {
      variants.push(option);
    } else {
      variants[id] = option;
    }

    this.setState({
      variants,
      editOption: null,
    });
  }

  @autobind
  cancelEditOption() {
    this.setState({
      editOption: null,
    });
  }

  renderOptions(variants: { [key:string]: Variant }): Array<Element> {
    return _.map(variants, (value, key) => {
      const reactKey = `product-variant-${key}`;
      return (
        <OptionEntry
          key={reactKey}
          id={key}
          option={value}
          editOption={this.startEditOption}
          deleteOption={this.deleteOption}
          confirmAction={this.updateOption}
        />
      );
    });
  }

  render(): Element {
    const variants = this.renderOptions(this.state.variants);
    const content = _.isEmpty(variants) ? this.emptyContent : variants;
    const optionDialog = (
      <OptionEditDialog
        option={this.state.editOption}
        cancelAction={this.cancelEditOption}
        confirmAction={this.updateOption}
      />
    );

    return (
      <ContentBox title="Options" actionBlock={this.actions}>
        {content}
        {this.state.editOption && optionDialog}
      </ContentBox>
    );
  }
}

export default OptionList;
