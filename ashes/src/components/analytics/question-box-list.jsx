// @flow

// libs
import React, { Component, Element } from 'react';
import _ from 'lodash';
import { autobind } from 'core-decorators';

// components
import QuestionBox, { Props as QuestionBoxType } from './question-box';

// types
export type Props = {
  items: Array<QuestionBoxType>,
  onSelect?: ?Function,
};

class QuestionBoxList extends Component {

  props: Props;

  static defaultProps = {
    items: [],
    onSelect: null,
  };

  @autobind
  renderItems(): ?Element {
    const { items, onSelect } = this.props;

    return _.map(items, (item, index) => (
      <QuestionBox
        id={index}
        title={item.title}
        content={item.content}
        footer={item.footer}
        isActive={item.isActive}
        onClick={onSelect}
        key={`question-list-${index}`}
      />
    ));
  }

  render() {
    return (
      <div>
        {this.renderItems()}
      </div>
    );
  }
}

export default QuestionBoxList;