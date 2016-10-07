/**
 * @flow
 */

import React, { Component, Element } from 'react';
import { autobind } from 'core-decorators';
import _ from 'lodash';
import { assoc, dissoc } from 'sprout-data';

// components
import ContentBox from 'components/content-box/content-box';
import OptionEntry from './option-entry';
import OptionEditDialog from './option-edit-dialog';

// styles
import styles from './option-list.css';

// types
import type { Option } from 'paragons/product';

type Props = {
  variants: Array<Option>,
  updateVariants: Function,
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
        This product does not have options.
      </div>
    );
  }

  @autobind
  startEditOption(id: any): void {
    const option = (id !== 'new') ? this.props.variants[id] : {
      attributes: {
        name: {
          t: 'string',
          v: ''
        },
        type: {
          t: 'string',
          v: '',
        }
      },
      values: [],
    };

    const editOption = { id, option };

    this.setState({
      editOption
    });
  }

  @autobind
  deleteOption(id: number): void {
    const { variants } = this.props;

    const newVariants = variants.slice();
    newVariants.splice(id, 1);

    this.props.updateVariants(newVariants);
  }

  @autobind
  updateOption(id: string|number, option: Option): void {
    const { variants } = this.props;

    const newVariants = id == 'new' ? [...variants, option] : assoc(variants, id, option);

    this.setState({
      editOption: null,
    }, () => this.props.updateVariants(newVariants));
  }

  @autobind
  cancelEditOption(): void {
    this.setState({
      editOption: null,
    });
  }

  renderOptions(variants: Array<Option>): Array<Element> {
    return _.map(variants, (option, key) => {
      const reactKey = `product-variant-${key}`;
      return (
        <OptionEntry
          key={reactKey}
          id={key}
          option={option}
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

    const optionDialog = this.state.editOption && (
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
