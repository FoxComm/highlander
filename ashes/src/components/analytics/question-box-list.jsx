// @flow

// libs
import React, { Component } from 'react';
import _ from 'lodash';
import { autobind } from 'core-decorators';

// components
import QuestionBox, { Props as QuestionBoxType } from './question-box';

// types
export type Props = {
  items: Array<QuestionBoxType>,
  onSelect?: ?Function,
  activeQuestion?: QuestionBoxType,
};

class QuestionBoxList extends Component {

  props: Props;

  static defaultProps = {
    items: [],
    onSelect: null,
    activeQuestion: null,
  };

  @autobind
  renderItems() {
    const { items, onSelect, activeQuestion } = this.props;

    return _.map(items, (item, index) => (
      <QuestionBox
        id={item.id}
        title={item.title}
        content={item.content}
        footer={item.footer}
        isActive={(activeQuestion.id === item.id)}
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
