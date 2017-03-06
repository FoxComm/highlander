/* @flow */

// libs
import React, { Component } from 'react';

// components
import FAQ from '../../components/faqs/faq';

// styles
import styles from './faq.css';

type Props = {
  title: string,
  faqs: Array<Object>
};

const generateKey = (): string => {
  return Math.random().toString(36).substring(7).toUpperCase();
};

class FAQSection extends Component {
  props: Props;

  render() {
    return (
      <div>
        <h2 styleName="title">{this.props.title}</h2>
        {this.props.faqs.map(
          qa =>
              <FAQ
                question={qa.question}
                answer={qa.answer}
                key={generateKey()}
              />
        )}
      </div>
    );
  }
}

export default FAQSection;
