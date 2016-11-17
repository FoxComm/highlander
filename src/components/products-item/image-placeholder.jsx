/* @flow */

// libs
import React from 'react';
import classnames from 'classnames';

// styles
import styles from './image-placeholder.css';

type Props = {
  largeScreenOnly?: boolean,
};

const ImagePlaceholder = (props: Props) => {
  const classNames = classnames({
    [styles.largeScreenOnly]: props.largeScreenOnly,
  });

  return (
    <div styleName="image-placeholder" className={classNames}>
      <span>Image coming soon!</span>
    </div>
  );
};

export default ImagePlaceholder;
