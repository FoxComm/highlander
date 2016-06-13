/**
 * @flow
 */

import React, { Component, Element, PropTypes } from 'react';
import { autobind } from 'core-decorators';
import _ from 'lodash';
import { illuminateAttributes, setAttribute } from '../../paragons/form-shadow-object';

import RoundedPill from '../rounded-pill/rounded-pill';
import TextInput from '../forms/text-input';

import styles from './tags.css';

type Attribute = { t: string, v: any };
type Attributes = { [key:string]: Attribute };

type Props = {
  attributes: Attributes,
  onChange: (form: FormAttributes, shadow: ShadowAttributes) => void,
};

type State = {
  isAdding: bool;
  addingValue: string,
};

export default class Tags extends Component<void, Props, State> {
  state: State = { isAdding: false, addingValue: '' };
  props: Props;

  get addInput(): ?Element {
    if (this.state.isAdding) {
      return (
        <TextInput
          styleName="text-input"
          placeholder="Separate tags with a comma"
          onBlur={this.submitTags}
          onKeyDown={this.handleKeyDown}
          onChange={this.handleChange}
          value={this.state.addingValue} />
      );
    }
  }

  get tags(): Array<string> {
    const { attributes } = this.props;
    return _.get(attributes, 'tags.v', []);
  }

  @autobind
  submitTags() {
    const tags = _.compact(this.state.addingValue.trim().split(',').map(s => s.trim()));
    const nTags = _.unique([...this.tags, ...tags]);

    this.setState({
      isAdding: false,
      addingValue: '',
    }, () => this.updateTags(nTags));
  }

  @autobind
  handleKeyDown(event: Object) {
    const {key} = event;
    if (key === 'Enter') {
      this.submitTags();
    } else if (key === 'Escape') {
      this.setState({
        isAdding: false,
      });
    }
  }

  @autobind
  handleChange(addingValue: string) {
    this.setState({ addingValue });
  }

  @autobind
  handleTagToggle() {
    this.setState({ isAdding: !this.state.isAdding });
  }

  @autobind
  handleRemoveTag(value: string) {
    const tags = _.reject(this.tags, tag => tag === value);
    this.updateTags(tags);
  }

  updateTags(tags: Array<string>) {
    const { attributes } = this.props;
    const newAttr = { t: 'tags', v: tags };
    this.props.onChange({ ...attributes, tags: newAttr });
  }

  render(): Element {
    const tags = this.tags;
    const mainContent = _.isEmpty(tags)
      ? <div styleName="empty-text">Add a tag</div>
      : tags.map(tag => {
        return <RoundedPill styleName="tag" text={tag} value={tag} onClose={this.handleRemoveTag} key={tag} />;
      });

    return (
      <div styleName="main">
        <div styleName="header">
          <div styleName="text">
            Tags
          </div>
          <button styleName="icon" onClick={this.handleTagToggle}>
            <i className="icon-add" />
          </button>
        </div>
        {this.addInput}
        <div styleName="tags">{mainContent}</div>
      </div>
    );
  }
}
