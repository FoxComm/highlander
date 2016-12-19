/* @flow */

// libs
import React from 'react';
import { Link } from 'react-router';

// styles
import styles from './action-block.css';

type Props = {
  imageUrl: string,
  description: string,
  title: string,
  action: {
    title: string,
    link: string,
  },
};


const ActionBlock = (props: Props) => {
  const bgImageStyle = { backgroundImage: `url(${props.imageUrl})` };
  const { description, title, action } = props;

  return (
    <div styleName="header" style={bgImageStyle}>
      <div styleName="header-wrap">
        <div styleName="text-wrap">
          <h1 styleName="title">{title}</h1>
          <span styleName="description">{description}</span>
        </div>
        <Link to={action.link} styleName="action-link">
          {action.title}
        </Link>
      </div>
    </div>
  );
};

export default ActionBlock;
