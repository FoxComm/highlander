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
      <a styleName="add-icon" onClick={() => this.editOption('new')}>
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
  editOption(id) {
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
  handleSaveOption(option, id) {
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
  handleCancelEditOption() {
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
          variant={value}
          editOption={this.editOption}
        />
      );
    });
  }

  render(): Element {
    const variants = this.renderOptions(this.state.variants);
    const content = _.isEmpty(variants) ? this.emptyContent : variants;
    const editDialog = (
      <OptionEditDialog
        option={this.state.editOption}
        cancelAction={this.handleCancelEditOption}
        confirmAction={this.handleSaveOption}
      />
    );

    return (
      <ContentBox title="Options" actionBlock={this.actions}>
        {content}
        {this.state.editOption !== null && editDialog}
      </ContentBox>
    );
  }
}

export default OptionList;
