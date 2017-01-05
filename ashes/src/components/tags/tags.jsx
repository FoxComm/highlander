/**
 * @flow
 */

import React, { Component, Element, PropTypes } from 'react';
import { autobind } from 'core-decorators';
import _ from 'lodash';
import { trackEvent } from 'lib/analytics';

import RoundedPill from '../rounded-pill/rounded-pill';
import TextInput from '../forms/text-input';

import styles from './tags.css';

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
  handleChange(addingValue: string) {
    this.setState({ addingValue });
  }

  @autobind
  handleTagToggle() {
    this.trackEvent('click_toggle_adding');
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
          <button id="tag-toggle-btn" styleName="icon" onClick={this.handleTagToggle}>
            <i className="icon-add" />
          </button>
        </div>
        {this.addInput}
        <div styleName="tags">{mainContent}</div>
      </div>
    );
  }
}
