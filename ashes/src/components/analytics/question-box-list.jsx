// @flow

// libs
import React, { Component, Element } from 'react';
import _ from 'lodash';
import { autobind } from 'core-decorators';

// components
import QuestionBox from './question-box';
import type { Props as QuestionBoxType } from './question-box';

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
    onSelect: _.noop,
    activeQuestion: null,
  };

  @autobind
  renderItems(): ?Element {
    const { items, onSelect, activeQuestion } = this.props;
    let activeQuestionId = false;

    if(activeQuestion !== undefined && _.isNull(activeQuestion)) {
      activeQuestionId = activeQuestion.id;
    }

    return _.map(items, (item, index) => (
      <QuestionBox
        id={index}
        title={item.title}
        content={item.content}
        footer={item.footer}
        isActive={(activeQuestionId === index)}
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
