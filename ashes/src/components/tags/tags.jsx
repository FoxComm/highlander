/**
 * @flow
 */

import React, { Component, Element } from 'react';
import { autobind } from 'core-decorators';
import _ from 'lodash';
import { trackEvent } from 'lib/analytics';

import { RoundedPill } from 'components/core/rounded-pill';
import TextInput from 'components/core/text-input';

import styles from './tags.css';

import type { Value } from 'components/core/rounded-pill';

type Props = {
  attributes: Attributes,
  onChange: (attributes: Attributes) => void,
  parent?: string,
};

type State = {
  isAdding: boolean;
  addingValue: string,
};

export default class Tags extends Component {
  state: State = { isAdding: false, addingValue: '' };
  props: Props;

  get addInput(): ?Element<*> {
    if (this.state.isAdding) {
      return (
        <TextInput
          styleName="text-input"
          placeholder="Separate tags with a comma"
          onBlur={this.submitTags}
          onKeyDown={this.handleKeyDown}
          onChange={this.handleChange}
          value={this.state.addingValue}
          autoFocus />
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
    const nTags = _.uniq([...this.tags, ...tags]);

    this.setState({
      isAdding: false,
      addingValue: '',
    }, () => this.updateTags(nTags));
  }

  trackEvent(...args: any[]) {
    trackEvent(`Tags(${this.props.parent || ''})`, ...args);
  }

  @autobind
  handleKeyDown(event: Object) {
    const {key} = event;
    if (key === 'Enter') {
      this.trackEvent('hit_enter');
      this.submitTags();
    } else if (key === 'Escape') {
      this.trackEvent('hit_escape');
      this.setState({
        isAdding: false,
      });
    }
  }

  @autobind
  handleChange(target: HTMLInputElement) {
    this.setState({ addingValue: target.value });
  }

  @autobind
  handleTagToggle() {
    this.trackEvent('click_toggle_adding');
    this.setState({ isAdding: !this.state.isAdding });
  }

  @autobind
  handleRemoveTag(value: Value) {
    const tags = _.reject(this.tags, tag => tag === String(value));
    this.updateTags(tags);
  }

  updateTags(tags: Array<string>) {
    const { attributes } = this.props;
    const newAttr = { t: 'tags', v: tags };
    this.props.onChange({ ...attributes, tags: newAttr });
  }

  render() {
    const tags = this.tags;
    const mainContent = _.isEmpty(tags)
      ? <div styleName="empty-text">Add a tag</div>
      : tags.map(tag => {
        const tagVal = _.kebabCase(tag);
        return (
          <RoundedPill
            pillId={`fct-tag__${tagVal}`}
            styleName="tag"
            text={tag}
            value={tag}
            onClose={this.handleRemoveTag}
            key={tag}
          />
        );
      });

    return (
      <div styleName="main">
        <div styleName="header">
          <div styleName="text">
            Tags
          </div>
          <button id="fct-tag-toggle-btn" styleName="icon" onClick={this.handleTagToggle}>
            <i className="icon-add" />
          </button>
        </div>
        {this.addInput}
        <div styleName="tags">{mainContent}</div>
      </div>
    );
  }
}
