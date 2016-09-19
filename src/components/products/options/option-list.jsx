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
import type { Option } from 'paragons/product';

type Props = {
  variants: Array<Option>,
};

type EditOption = {
  id: string|number,
  option: Option,
};

type State = {
  editOption: ?EditOption,
};

class OptionList extends Component {
  props: Props;

  state: State = {
    editOption: null,
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
  startEditOption(id: string|number) {
    let editOption = { id };

    if (id !== 'new') {
      editOption.option = this.props.variants[id]
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
  deleteOption(id: string|number) {
    const { variants } = this.props;

    variants.splice(id, 1);

    this.props.updateVariants(variants);
  }

  @autobind
  updateOption(option: Option, id: string|number) {
    const { variants } = this.props;

    if (id === 'new') {
      variants.push(option);
    } else {
      variants[id] = option;
    }

    this.setState({
      editOption: null,
    }, () => this.props.updateVariants(variants));
  }

  @autobind
  cancelEditOption() {
    this.setState({
      editOption: null,
    });
  }

  renderOptions(variants: Array<Option>): Array<Element> {
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
    const variants = this.renderOptions(this.props.variants);
    const content = _.isEmpty(variants) ? this.emptyContent : variants;
    const editOps = _.get(this.state, 'editOption'); // flow...
    console.log(this.props);

    const optionDialog = (
      <OptionEditDialog
        option={editOps}
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
