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

  return (
    <div styleName="header" style={bgImageStyle}>
      <div styleName="header-wrap">
        <div styleName="text-wrap">
          <span styleName="description">{props.description}</span>
          <h1 styleName="title">{props.title}</h1>
          <Link to={props.action.link} styleName="action-link">
            {props.action.title}
          </Link>
        </div>
      </div>
    </div>
  );
};

export default ActionBlock;
