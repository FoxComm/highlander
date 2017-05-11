
import _ from 'lodash';
import React from 'react';

import { Link } from 'react-router';

import styles from '../profile.css';

const renderActions = (isNew) => {
  let actionsContent;
  if (isNew) {
    actionsContent = (
      <div styleName="actions-block">
        <Link styleName="link" to={`/profile`}>ADD</Link>
        &nbsp;|&nbsp;
        <Link styleName="link" to={`/profile`}>IGNORE</Link>
      </div>
    );
  } else {
    actionsContent = (
      <div styleName="actions-block">
        <Link styleName="link" to={`/profile`}>EDIT</Link>
        &nbsp;|&nbsp;
        <Link styleName="link" to={`/profile`}>REMOVE</Link>
      </div>
    );
  }
  return actionsContent;
};

const ReviewRow = (props) => {
  const { product, date, status, isNew } = props.review;
  return (
    <tr>
      <td>{product}</td>
      <td>{date}</td>
      <td>{status}</td>
      <td>{renderActions(isNew)}</td>
    </tr>
  );
};

export default ReviewRow;
