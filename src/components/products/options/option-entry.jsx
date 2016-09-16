/**
 * @flow
 */

// libs
import _ from 'lodash';
import React, { Component, Element } from 'react';
import { autobind } from 'core-decorators';
import { assoc } from 'sprout-data';

// components
import ContentBox from 'components/content-box/content-box';
import ValueEntry from './value-entry';
import ValueEditDialog from './value-edit-dialog';

// styles
import styles from './option-list.css';

// types
import type { Variant, VariantValue } from 'paragons/product';

type Props = {
  id: string,
  option: ?Variant,
  editOption: Function,
  deleteOption: Function,
  editValues: Function,
};

class OptionEntry extends Component {
  props: Props;

  state = {
    editValue: null,
  };

  get content() {
    const values = this.values;
    if (_.isEmpty(values)) {
      return (
        <div className="fc-content-box__empty-text">
          This option does not have values applied.
        </div>
      );
    } else {
      const optionName = _.get(this.props, 'option', '');
      const entries = _.map(this.values, (value, name) => {
        const key = `product-option-${optionName}-${name}`;
        return <ValueEntry key={key} name={name} value={value} />;
      });

      return (
        <div className="fc-option-entry">
          <table className="fc-table">
            <tbody>
              {entries}
            </tbody>
          </table>
        </div>
      );
    }
  }

  get titleBar(): Element {
    const name = _.get(this.props, 'option.name');
    const type = _.get(this.props, 'option.type');

    return (
      <div className="fc-variant-entry__title-bar">
        <div className="fc-variant-entry__name">{name}</div>
        <div className="fc-variant-entry__type">{type}</div>
      </div>
    );
  }

  get titleBarActions():Element {
    return (
      <div className="fc-variant-entry__title-bar-actions">
        <a onClick={() => this.editValue('new')} styleName="action-icon"><i className="icon-add"/></a>
        <a onClick={() => this.props.editOption(this.props.id)} styleName="action-icon"><i className="icon-edit"/></a>
        <a onClick={() => this.props.deleteOption(this.props.id)} styleName="action-icon"><i className="icon-trash"/></a>
      </div>
    );
  }

  get values(): { [key:string]: VariantValue } {
    return _.get(this.props, 'option.values', {});
  }

  @autobind
  editValue(id) {
    this.setState({
      editValue: {
        id,
        value: {
          name: '',
          swatch: '',
        }
      }
    })
  };

  @autobind
  updateValue(value, id) {
    const values = _.get(this.props.option, 'values', []);

    if (id === 'new') {
      values.push(value);
    } else {
      values[id] = value;
    }

    const option = assoc(this.props.option, 'values', values);

    this.props.confirmAction(option, this.props.id);

    this.setState({ editValue: null })
  }

  @autobind
  cancelEdit() {
    this.setState({ editValue: null })
  }

  render(): Element {
    const valueDialog = (
      <ValueEditDialog
        value={this.state.editValue}
        cancelAction={this.cancelEdit}
        confirmAction={this.updateValue}
      />
    );
    return (
      <ContentBox
        title={this.titleBar}
        actionBlock={this.titleBarActions}
        indentContent={false}>
        {this.content}
        {this.state.editValue && valueDialog}
      </ContentBox>
    );
  }
}

export default OptionEntry;
