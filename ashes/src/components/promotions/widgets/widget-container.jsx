
/* @flow */

import React, { Element} from 'react';

import styles from './widget-container.css';

type Props = {
  children: Element<*>;
}

const WidgetContainer = (props: Props) => {
  return (
    <div styleName="container">
      {React.Children.map(props.children, node => {
        return React.isValidElement(node) ? node : <span>{node}</span>;
      })}
    </div>
  );
};

export default WidgetContainer;
